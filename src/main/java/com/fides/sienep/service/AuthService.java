package com.fides.sienep.service;

import com.fides.sienep.dto.LoginRequestDTO;
import com.fides.sienep.dto.LoginResponseDTO;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.LogIntentoRepository;
import com.fides.sienep.repository.UsuarioRepository;
import com.fides.sienep.utils.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int  MAX_INTENTOS_FALLIDOS = 5;
    private static final int  VENTANA_MINUTOS       = 10;

    private final AuthenticationManager    authManager;
    private final JwtTokenUtil             jwtTokenUtil;
    private final FuncionarioRepository    funcionarioRepository;
    private final EstudianteRepository     estudianteRepository;
    private final UsuarioRepository        usuarioRepository;
    private final LogIntentoRepository     logIntentoRepository;
    private final PasswordEncoder          passwordEncoder;
    private final EmailService             emailService;
    private final PasswordGeneratorService passwordGeneratorService;
    private final AuditoriaService         auditoriaService;

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

    @Transactional
    public void olvideMiContrasenia(String email) {
        // Respuesta genérica siempre — no revelar si el email existe o no
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email.toLowerCase());
        if (usuarioOpt.isEmpty()) {
            log.warn("Solicitud de olvido para email inexistente: {}", email);
            return;
        }

        Usuario usuario = usuarioOpt.get();
        String contraseniaPlana = passwordGeneratorService.generar();

        usuario.setContrasenia(passwordEncoder.encode(contraseniaPlana));
        usuario.setBloqueado(false);
        usuarioRepository.save(usuario);

        emailService.enviarContraseniaRestablecida(usuario.getEmail(), usuario.getNombre(), contraseniaPlana);
        log.info("[AUDIT] Contraseña restablecida por olvido para id={}", usuario.getIdUsuario());
    }

    // =========================================================
    // PRIVADOS
    // =========================================================

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