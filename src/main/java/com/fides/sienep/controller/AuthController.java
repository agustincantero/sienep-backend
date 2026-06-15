package com.fides.sienep.controller;

import com.fides.sienep.dto.LoginRequestDTO;
import com.fides.sienep.dto.LoginResponseDTO;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.utils.JwtTokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import com.fides.sienep.service.AuditoriaService;
import com.fides.sienep.model.TipoAccion;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login y logout del sistema")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final FuncionarioRepository funcionarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final AuditoriaService auditoriaService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
            description = "Autentica un funcionario o estudiante y devuelve un token JWT válido por 5 horas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto, HttpServletRequest request) {

        // 1. Resolver quién es — funcionario o estudiante
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
            rol = "ESTUDIANTE"; // rol sintético
        }

        // 2. Validar contraseña (usa LoginUsuarioDetailsService internamente)
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.getEmail(), dto.getContrasenia()));
        } catch (BadCredentialsException e) {
            log.warn("Login fallido para id: {}", idUsuario);
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        // 3. Generar token
        String token = jwtTokenUtil.generateToken(idUsuario, rol);
        log.info("Login exitoso: id={} rol={}", idUsuario, rol);

        // --- AUDITORIA ---
        auditoriaService.registrarEventoManual("SISTEMA", TipoAccion.LOGIN, idUsuario, request.getRemoteAddr());

        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión",
            description = "Stateless: el cliente descarta el token.")
    @ApiResponse(responseCode = "200", description = "Sesión cerrada")
    public ResponseEntity<Void> logout(HttpServletRequest request, Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            try {
                int idUsuario = Integer.parseInt(auth.getName());
                auditoriaService.registrarEventoManual("SISTEMA", TipoAccion.LOGOUT, idUsuario, request.getRemoteAddr());
            } catch (Exception e) {
                log.warn("No se pudo auditar el logout: " + e.getMessage());
            }
        }
        return ResponseEntity.ok().build();
    }
}