package com.fides.sienep.service;

import com.fides.sienep.dto.AdLoginRequestDTO;
import com.fides.sienep.dto.GoogleLoginRequestDTO;
import com.fides.sienep.dto.LoginRequestDTO;
import com.fides.sienep.dto.LoginResponseDTO;
import com.fides.sienep.dto.UsuarioAutenticadoDTO;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.LogIntentoRepository;
import com.fides.sienep.repository.TokenRestablecimientoRepository;
import com.fides.sienep.repository.UsuarioRepository;
import com.fides.sienep.utils.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int  MAX_INTENTOS_FALLIDOS  = 5;
    private static final int  VENTANA_MINUTOS        = 10;
    private static final int  TOKEN_VALIDEZ_MINUTOS  = 30;

    // Hash BCrypt válido de una contraseña que no corresponde a ningún usuario real.
    // Se usa únicamente para forzar el mismo costo de cómputo que un login con
    // contraseña incorrecta cuando el email ni siquiera existe (ver login()) —
    // de lo contrario, la ausencia de ese costo es un oráculo de timing que permite
    // enumerar cuentas registradas sin necesidad de acertar ninguna contraseña.
    private static final String HASH_DUMMY_TIMING =
            new BCryptPasswordEncoder().encode("valor-fijo-sin-uso-real-solo-para-timing");

    private final AuthenticationManager           authManager;
    private final JwtTokenUtil                    jwtTokenUtil;
    private final FuncionarioRepository           funcionarioRepository;
    private final EstudianteRepository            estudianteRepository;
    private final UsuarioRepository               usuarioRepository;
    private final LogIntentoRepository            logIntentoRepository;
    private final TokenRestablecimientoRepository tokenRestablecimientoRepository;
    private final PasswordEncoder                 passwordEncoder;
    private final EmailService                    emailService;
    private final AuditoriaService                auditoriaService;
    private final PasswordResetRateLimiter        passwordResetRateLimiter;
    private final LoginIpRateLimiter              loginIpRateLimiter;
    private final GoogleIdTokenVerifierService    googleIdTokenVerifierService;
    private final ActiveDirectoryVerifierService  activeDirectoryVerifierService;

    // Campo separado (no final): Lombok no copia @Value a los parámetros del
    // constructor generado por @RequiredArgsConstructor sin un lombok.config,
    // por lo que esta property se inyecta directamente sobre el campo.
    @Value("${app.frontend.url}")
    private String frontendUrl;

    // =========================================================
    // LOGIN
    // =========================================================

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResponseDTO login(LoginRequestDTO dto, String ip) {

        // Mismo criterio que olvideMiContrasenia(): el trigger de BD normaliza el
        // email a minúsculas al guardar, así que la búsqueda tiene que normalizar
        // igual — de lo contrario, un usuario que loguea con el email en mayúsculas
        // nunca encuentra su cuenta aunque la contraseña sea correcta.
        String emailNormalizado = dto.getEmail().toLowerCase();

        // 1. Resolver quién es
        int idUsuario;
        String rol;

        Optional<Funcionario> funcionarioOpt = funcionarioRepository.findByEmail(emailNormalizado);
        if (funcionarioOpt.isPresent()) {
            idUsuario = funcionarioOpt.get().getIdUsuario();
            rol = funcionarioOpt.get().getRol().getNombre();
        } else {
            Optional<Estudiante> estudianteOpt = estudianteRepository.findByEmail(emailNormalizado);
            if (estudianteOpt.isEmpty()) {
                // Igualar el tiempo de respuesta con el caso "contraseña incorrecta"
                // (que sí ejecuta BCrypt más abajo vía authManager.authenticate()).
                passwordEncoder.matches(dto.getContrasenia(), HASH_DUMMY_TIMING);
                throw credencialesInvalidas(ip);
            }
            idUsuario = estudianteOpt.get().getIdUsuario();
            rol = estudianteOpt.get().getRol().getNombre();
        }

        // 2. Verificar si está bloqueado
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> credencialesInvalidas(ip));

        // Mismo status y mensaje que "credenciales inválidas" (no un 403 con
        // mensaje explícito): de lo contrario, un atacante puede distinguir "el
        // email no existe / la contraseña está mal" de "el email existe y está
        // bloqueado" con solo mirar el código de respuesta — un oráculo de
        // enumeración de cuentas. La recomendación de "Olvidé mi contraseña" para
        // desbloquear puede mostrarse como texto estático en el formulario de
        // login del frontend, no depender de si ESTE email puntual está bloqueado.
        if (usuario.isBloqueado()) {
            log.warn("Login denegado — usuario bloqueado id={}", idUsuario);
            throw credencialesInvalidas(ip);
        }

        // 3. Validar contraseña
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(emailNormalizado, dto.getContrasenia()));
        } catch (BadCredentialsException e) {
            registrarIntento(idUsuario, false);
            verificarYBloquearSiCorresponde(idUsuario, usuario);
            log.warn("Login fallido para id={}", idUsuario);
            throw credencialesInvalidas(ip);
        }

        // 4. Login exitoso
        registrarIntento(idUsuario, true);
        String token = jwtTokenUtil.generateToken(idUsuario, rol);
        log.info("[AUDIT] Login exitoso: id={} rol={}", idUsuario, rol);
        auditoriaService.registrarEventoManual("SISTEMA", TipoAccion.LOGIN, idUsuario, ip);

        return new LoginResponseDTO(token);
    }

    // =========================================================
    // LOGIN CON GOOGLE (OAuth 2.0)
    // =========================================================

    // Solo autentica: no da de alta cuentas nuevas. El email que Google ya
    // verificó tiene que coincidir con un Funcionario/Estudiante existente en
    // SIENEP (el alta la sigue haciendo un administrador, como hoy) — Google
    // reemplaza la contraseña como factor de login, no el proceso de alta.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResponseDTO loginConGoogle(GoogleLoginRequestDTO dto, String ip) {
        String email = googleIdTokenVerifierService.emailVerificado(dto.getIdToken())
                .orElseThrow(() -> credencialesInvalidas(ip));
        return loginConIdentidadExterna(email, ip, false, "Google");
    }

    // =========================================================
    // LOGIN CON ACTIVE DIRECTORY (LDAP)
    // =========================================================

    // Mismo patrón que loginConGoogle(): el controlador de dominio prueba quién es la persona y SIENEP solo busca la cuenta existente por el atributo mail de AD y emite su propio token. A diferencia de Google, acá un login exitoso sí saca a la cuenta de PENDIENTE_DE_ACTIVACION: la contraseña temporal local no interviene en este flujo, así que dejarla pendiente forzaría una pantalla de cambio de contraseña que un usuario de AD nunca necesita.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResponseDTO loginConAd(AdLoginRequestDTO dto, String ip) {
        String email = activeDirectoryVerifierService.emailVerificado(dto.getUsuario(), dto.getContrasenia()).orElseThrow(() -> credencialesInvalidas(ip));
        return loginConIdentidadExterna(email, ip, true, "AD");
    }

    // Compartido por loginConGoogle() y loginConAd(): ambos ya recibieron un email verificado por un proveedor externo, así que solo queda resolver la cuenta de SIENEP, activarla si correspondiera, validar que no esté bloqueada y emitir el mismo JWT que el resto de los logins. Mismo criterio de activo que LoginUsuarioDetailsService.loadUserByUsername(), reimplementado acá porque este flujo no pasa por authManager.authenticate() (no hay contraseña que validar, el proveedor externo ya probó la identidad).
    private LoginResponseDTO loginConIdentidadExterna(String email, String ip, boolean activarPendientes, String proveedor) {
        int idUsuario;
        String rol;
        boolean activa;
        boolean pendiente;

        Optional<Funcionario> funcionarioOpt = funcionarioRepository.findByEmail(email);
        if (funcionarioOpt.isPresent()) {
            Funcionario funcionario = funcionarioOpt.get();
            pendiente = funcionario.getEstado() == EstadoLogico.PENDIENTE_DE_ACTIVACION;
            activa = funcionario.getEstado() == EstadoLogico.ACTIVO || pendiente;
            if (activarPendientes && pendiente) {
                funcionario.setEstado(EstadoLogico.ACTIVO);
                funcionarioRepository.save(funcionario);
            }
            idUsuario = funcionario.getIdUsuario();
            rol = funcionario.getRol().getNombre();
        } else {
            Estudiante estudiante = estudianteRepository.findByEmail(email).orElseThrow(() -> credencialesInvalidas(ip));
            pendiente = estudiante.getEstado() == EstadoLogico.PENDIENTE_DE_ACTIVACION;
            activa = estudiante.getEstado() == EstadoLogico.ACTIVO || pendiente;
            if (activarPendientes && pendiente) {
                estudiante.setEstado(EstadoLogico.ACTIVO);
                estudianteRepository.save(estudiante);
            }
            idUsuario = estudiante.getIdUsuario();
            rol = estudiante.getRol().getNombre();
        }

        if (!activa) {
            log.warn("Login con {} denegado — cuenta inactiva id={}", proveedor, idUsuario);
            throw credencialesInvalidas(ip);
        }

        // Verificar si está bloqueado (mismo chequeo que login())
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> credencialesInvalidas(ip));

        if (usuario.isBloqueado()) {
            log.warn("Login con {} denegado — usuario bloqueado id={}", proveedor, idUsuario);
            throw credencialesInvalidas(ip);
        }

        registrarIntento(idUsuario, true);
        String token = jwtTokenUtil.generateToken(idUsuario, rol);
        log.info("[AUDIT] Login con {} exitoso: id={} rol={}", proveedor, idUsuario, rol);
        auditoriaService.registrarEventoManual("SISTEMA", TipoAccion.LOGIN, idUsuario, ip);

        return new LoginResponseDTO(token);
    }

    // =========================================================
    // USUARIO AUTENTICADO (GET /auth/me)
    // =========================================================

    // Identidad + permisos del dueño del token, para que el frontend gatee la
    // navegación y la UI. Sin parámetros: el id sale del SecurityContext (sub del
    // JWT ya validado por JwtRequestFilter), así que solo devuelve la cuenta
    // propia. Read-only, no genera auditoría.
    @Transactional(readOnly = true)
    public UsuarioAutenticadoDTO usuarioAutenticado() {
        int idUsuario = Integer.parseInt(
                SecurityContextHolder.getContext().getAuthentication().getName());

        Optional<Funcionario> funcionarioOpt = funcionarioRepository.findByIdConRolYPermisos(idUsuario);
        if (funcionarioOpt.isPresent()) {
            Funcionario f = funcionarioOpt.get();
            return toDto(f, f.getRol(), TipoUsuario.FUNCIONARIO);
        }

        Estudiante estudiante = estudianteRepository.findByIdConRolYPermisos(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuario del token no encontrado"));
        return toDto(estudiante, estudiante.getRol(), TipoUsuario.ESTUDIANTE);
    }

    private UsuarioAutenticadoDTO toDto(Usuario usuario, Rol rol, TipoUsuario tipo) {
        return UsuarioAutenticadoDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .tipo(tipo)
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .estado(usuario.getEstado())
                .rol(rol.getNombre())
                .permisos(permisosActivos(rol))
                .build();
    }

    // Mismo criterio que CustomUsuarioDetailsService: solo RolPermiso ACTIVO, y
    // el nom_permiso (sin el prefijo ROLE_ que sí lleva la GrantedAuthority).
    private List<String> permisosActivos(Rol rol) {
        return rol.getRolPermisos().stream()
                .filter(rp -> rp.getEstado() == EstadoLogico.ACTIVO)
                .map(rp -> rp.getPermiso().getNombre())
                .distinct()
                .sorted()
                .toList();
    }

    // Único punto de salida para todo login rechazado (email inexistente, usuario
    // no encontrado, bloqueado, o contraseña incorrecta): mismo status y mensaje en
    // los cuatro casos (ver nota de enumeración de cuentas más arriba), y de paso
    // registra el fallo para el rate-limit por IP (password-spraying).
    private ResponseStatusException credencialesInvalidas(String ip) {
        if (!loginIpRateLimiter.registrarIntento(ip)) {
            log.warn("Límite de intentos de login fallidos excedido para ip={}", ip);
            return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos de inicio de sesión fallidos. Intenta nuevamente más tarde.");
        }
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    // ⚠ LIMITACIÓN CONOCIDA — JWT stateless sin revocación (logout cosmético)
    // El JWT emitido en login tiene una validez de 5 horas (VALIDEZ_SEG en JwtTokenUtil)
    // y no existe ningún mecanismo de blacklist ni de revocación activa.
    // Este método solo deja registro de auditoría del evento: el token sigue siendo
    // criptográficamente válido y cualquier petición que lo incluya seguirá siendo
    // aceptada por JwtRequestFilter hasta que expire naturalmente.
    //
    // Opciones evaluadas para corregirlo en el futuro (requiere decisión del equipo):
    //   a) Tokens de vida corta (ej. 15 min) + refresh token de vida larga.
    //      Pro: sin estado extra en el servidor. Con: complejidad en el cliente.
    //   b) Blacklist de JTI en Redis o en tabla propia con TTL = VALIDEZ_SEG.
    //      Pro: revocación inmediata. Con: introduce estado del lado del servidor
    //      y rompe el modelo "stateless puro" que hoy simplifica el escalado.
    //
    // Por ahora se acepta la limitación como decisión de diseño consciente.
    // No implementar ninguna de estas opciones sin definir el alcance con el equipo.
    public void logout(Authentication auth, String ip) {
        if (auth != null && auth.isAuthenticated()
                && !auth.getPrincipal().equals("anonymousUser")) {
            try {
                int idUsuario = Integer.parseInt(auth.getName());
                auditoriaService.registrarEventoManual(
                        "SISTEMA", TipoAccion.LOGOUT, idUsuario, ip);
            } catch (Exception e) {
                log.warn("No se pudo auditar el logout: {}", e.getMessage());
            }
        }
    }

    // =========================================================
    // OLVIDÉ MI CONTRASEÑA
    // =========================================================

    // No autenticado (público), así que NO puede tocar la contraseña acá mismo — solo
    // genera un token de un solo uso y manda un link. La contraseña vieja sigue
    // funcionando hasta que alguien confirme el cambio con ese token en
    // restablecerContraseniaConToken(). Esto evita que el solo hecho de conocer el
    // email de otra persona alcance para tumbarle la sesión/cuenta (DoS sin login).
    @Transactional
    public void olvideMiContrasenia(String email) {
        String emailNormalizado = email.toLowerCase();

        // El límite se aplica por email (no por idUsuario) y ANTES de saber si el
        // usuario existe: si solo se limitaran los emails que sí existen, el 429
        // aparecería únicamente para cuentas reales y sería en sí mismo un oráculo
        // de enumeración (ver PasswordResetRateLimiter). La respuesta acá tiene que
        // ser la misma sin importar si el email pertenece a un usuario o no.
        if (!passwordResetRateLimiter.registrarIntento(emailNormalizado)) {
            log.warn("Límite de restablecimiento excedido para email={}", emailNormalizado);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Has solicitado restablecer la contraseña demasiadas veces. Por favor, intenta nuevamente más tarde.");
        }

        // Respuesta genérica siempre — no revelar si el email existe o no
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(emailNormalizado);
        if (usuarioOpt.isEmpty()) {
            log.warn("Solicitud de olvido para email inexistente: {}", email);
            return;
        }

        Usuario usuario = usuarioOpt.get();

        tokenRestablecimientoRepository.invalidarTokensActivos(usuario.getIdUsuario());

        String tokenPlano = UUID.randomUUID().toString();
        LocalDateTime expiracion = LocalDateTime.now().plusMinutes(TOKEN_VALIDEZ_MINUTOS);
        tokenRestablecimientoRepository.save(
                new TokenRestablecimiento(usuario.getIdUsuario(), hashToken(tokenPlano), expiracion));

        // El fallo de envío no debe revelar nada distinto de "email inexistente" — se
        // loguea y listo, la respuesta externa es la misma en ambos casos.
        try {
            String link = frontendUrl + "/reset-password?token=" + tokenPlano;
            emailService.enviarLinkRestablecimiento(usuario.getEmail(), usuario.getNombre(), link);
        } catch (Exception e) {
            log.warn("[AUDIT] No se pudo enviar el email de restablecimiento a {}: {}",
                    usuario.getEmail(), e.getMessage());
        }

        log.info("[AUDIT] Token de restablecimiento generado para id={}", usuario.getIdUsuario());
    }

    // Confirma el cambio iniciado en olvideMiContrasenia(). Recién acá se aplica la
    // contraseña nueva — elegida por el usuario, no generada por el servidor — y
    // solo si el token es válido, no vencido y no usado antes.
    @Transactional
    public void restablecerContraseniaConToken(String tokenPlano, String contraseniaNueva) {
        Optional<TokenRestablecimiento> tokenOpt = tokenRestablecimientoRepository
                .findByTokenHashAndUsadoFalse(hashToken(tokenPlano));
        if (tokenOpt.isEmpty() || !tokenOpt.get().getFechaExpiracion().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido o vencido");
        }
        TokenRestablecimiento token = tokenOpt.get();

        Usuario usuario = usuarioRepository.findById(token.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Token inválido o vencido"));

        usuario.setContrasenia(passwordEncoder.encode(contraseniaNueva));
        usuario.setBloqueado(false);
        usuario.setCredencialesVigentesDesde(LocalDateTime.now());
        // Mismo criterio que FuncionarioService/EstudianteService.cambiarContrasenia():
        // si el usuario estaba obligado a cambiar una contraseña temporal, elegir una acá
        // también lo saca de ese estado — si no, quedaría en loop infinito en la pantalla
        // obligatoria del frontend pese a tener ya una contraseña propia y funcional.
        if (EstadoLogico.PENDIENTE_DE_ACTIVACION.equals(usuario.getEstado())) {
            usuario.setEstado(EstadoLogico.ACTIVO);
        }
        usuarioRepository.save(usuario);

        token.setUsado(true);
        tokenRestablecimientoRepository.save(token);

        log.info("[AUDIT] Contraseña restablecida vía token para id={}", usuario.getIdUsuario());
    }

    // =========================================================
    // PRIVADOS
    // =========================================================

    // SHA-256 alcanza acá: el token ya es aleatorio de alta entropía (UUID), no una
    // contraseña elegida por una persona — no hace falta un hash lento tipo bcrypt,
    // y uno rápido permite buscarlo directo por igualdad en la base de datos.
    private String hashToken(String tokenPlano) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenPlano.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private void registrarIntento(Integer idUsuario, boolean exitoso) {
        logIntentoRepository.save(new LogIntento(idUsuario, exitoso));
    }

    private long contarIntentosFallidos(Integer idUsuario) {
        LocalDateTime desde = LocalDateTime.now().minusMinutes(VENTANA_MINUTOS);
        return logIntentoRepository.contarIntentosFallidosDesde(idUsuario, desde);
    }

    private void verificarYBloquearSiCorresponde(Integer idUsuario, Usuario usuario) {
        long fallidosRecientes = contarIntentosFallidos(idUsuario);
        if (fallidosRecientes >= MAX_INTENTOS_FALLIDOS) {
            usuario.setBloqueado(true);
            usuarioRepository.save(usuario);
            log.warn("[AUDIT] Usuario id={} bloqueado por {} intentos fallidos en {} minutos",
                    idUsuario, MAX_INTENTOS_FALLIDOS, VENTANA_MINUTOS);
        }
    }
}