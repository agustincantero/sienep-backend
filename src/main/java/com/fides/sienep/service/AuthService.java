package com.fides.sienep.service;

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

        // 1. Resolver quién es
        int idUsuario;
        String rol;

        Optional<Funcionario> funcionarioOpt = funcionarioRepository.findByEmail(dto.getEmail());
        if (funcionarioOpt.isPresent()) {
            idUsuario = funcionarioOpt.get().getIdUsuario();
            rol = funcionarioOpt.get().getRol().getNombre();
        } else {
            Estudiante estudiante = estudianteRepository.findByEmail(dto.getEmail())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
            idUsuario = estudiante.getIdUsuario();
            rol = "ESTUDIANTE";
        }

        // 2. Verificar si está bloqueado
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        if (usuario.isBloqueado()) {
            log.warn("Login denegado — usuario bloqueado id={}", idUsuario);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta ha sido bloqueada por demasiados intentos fallidos. Utilizá la opción 'Olvidé mi contraseña' para desbloquearla.");
        }

        // 3. Validar contraseña
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getContrasenia()));
        } catch (BadCredentialsException e) {
            registrarIntento(idUsuario, false);
            verificarYBloquearSiCorresponde(idUsuario, usuario);
            log.warn("Login fallido para id={}", idUsuario);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        // 4. Login exitoso
        registrarIntento(idUsuario, true);
        String token = jwtTokenUtil.generateToken(idUsuario, rol);
        log.info("[AUDIT] Login exitoso: id={} rol={}", idUsuario, rol);
        auditoriaService.registrarEventoManual("SISTEMA", TipoAccion.LOGIN, idUsuario, ip);

        return new LoginResponseDTO(token);
    }

    // =========================================================
    // LOGOUT
    // =========================================================

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
        // Respuesta genérica siempre — no revelar si el email existe o no
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email.toLowerCase());
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
        TokenRestablecimiento token = tokenRestablecimientoRepository
                .findByTokenHashAndUsadoFalse(hashToken(tokenPlano))
                .filter(t -> t.getFechaExpiracion().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Token inválido o vencido"));

        Usuario usuario = usuarioRepository.findById(token.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Token inválido o vencido"));

        usuario.setContrasenia(passwordEncoder.encode(contraseniaNueva));
        usuario.setBloqueado(false);
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