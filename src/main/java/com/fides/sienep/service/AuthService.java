package com.fides.sienep.service;

import com.fides.sienep.dto.GoogleLoginRequestDTO;
import com.fides.sienep.dto.LoginRequestDTO;
import com.fides.sienep.dto.LoginResponseDTO;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int  MAX_INTENTOS_FALLIDOS  = 5;
    private static final int  VENTANA_MINUTOS        = 10;
    private static final int  TOKEN_VALIDEZ_MINUTOS  = 30;

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
            Estudiante estudiante = estudianteRepository.findByEmail(emailNormalizado)
                    .orElseThrow(() -> credencialesInvalidas(ip));
            idUsuario = estudiante.getIdUsuario();
            rol = "ESTUDIANTE";
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

        // 1. Resolver quién es y si la cuenta está activa — mismo criterio que
        // LoginUsuarioDetailsService.loadUserByUsername(), reimplementado acá
        // porque este flujo no pasa por authManager.authenticate() (no hay
        // contraseña que validar, Google ya probó la identidad).
        int idUsuario;
        String rol;
        boolean activa;

        Optional<Funcionario> funcionarioOpt = funcionarioRepository.findByEmail(email);
        if (funcionarioOpt.isPresent()) {
            Funcionario funcionario = funcionarioOpt.get();
            idUsuario = funcionario.getIdUsuario();
            rol = funcionario.getRol().getNombre();
            activa = funcionario.getEstado() == EstadoLogico.ACTIVO
                    || funcionario.getEstado() == EstadoLogico.PENDIENTE_DE_ACTIVACION;
        } else {
            Estudiante estudiante = estudianteRepository.findByEmail(email)
                    .orElseThrow(() -> credencialesInvalidas(ip));
            idUsuario = estudiante.getIdUsuario();
            rol = "ESTUDIANTE";
            activa = estudiante.getEstado() == EstadoLogico.ACTIVO
                    || estudiante.getEstado() == EstadoLogico.PENDIENTE_DE_ACTIVACION;
        }

        if (!activa) {
            log.warn("Login con Google denegado — cuenta inactiva id={}", idUsuario);
            throw credencialesInvalidas(ip);
        }

        // 2. Verificar si está bloqueado (mismo chequeo que login())
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> credencialesInvalidas(ip));

        if (usuario.isBloqueado()) {
            log.warn("Login con Google denegado — usuario bloqueado id={}", idUsuario);
            throw credencialesInvalidas(ip);
        }

        // 3. Éxito
        registrarIntento(idUsuario, true);
        String token = jwtTokenUtil.generateToken(idUsuario, rol);
        log.info("[AUDIT] Login con Google exitoso: id={} rol={}", idUsuario, rol);
        auditoriaService.registrarEventoManual("SISTEMA", TipoAccion.LOGIN, idUsuario, ip);

        return new LoginResponseDTO(token);
    }

    // Único punto de salida para todo login rechazado (email inexistente, usuario
    // no encontrado, bloqueado, o contraseña incorrecta): mismo status y mensaje en
    // los cuatro casos (ver nota de enumeración de cuentas más arriba), y de paso
    // registra el fallo para el rate-limit por IP (password-spraying).
    private ResponseStatusException credencialesInvalidas(String ip) {
        if (!loginIpRateLimiter.registrarIntento(ip)) {
            log.warn("Límite de intentos de login fallidos excedido para ip={}", ip);
            return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos de inicio de sesión fallidos. Intentá nuevamente más tarde.");
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
                    "Has solicitado restablecer la contraseña demasiadas veces. Por favor, intentá nuevamente más tarde.");
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
            String link = frontendUrl + "/restablecer-contrasenia?token=" + tokenPlano;
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