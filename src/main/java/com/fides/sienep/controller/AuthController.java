package com.fides.sienep.controller;

import com.fides.sienep.dto.LoginRequestDTO;
import com.fides.sienep.dto.LoginResponseDTO;
import com.fides.sienep.dto.OlvideContraseniaDTO;
import com.fides.sienep.model.TipoAccion;
import com.fides.sienep.service.AuditoriaService;
import com.fides.sienep.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login, logout y recuperación de contraseña")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService      authService;
    private final AuditoriaService auditoriaService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
            description = "Autentica un funcionario o estudiante y devuelve un token JWT válido por 5 horas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
            @ApiResponse(responseCode = "403", description = "Cuenta bloqueada")
    })
    public ResponseEntity<LoginResponseDTO> login(
            @RequestBody @Valid LoginRequestDTO dto,
            HttpServletRequest request) {

        LoginResponseDTO response = authService.login(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión",
            description = "Stateless: el cliente descarta el token.")
    @ApiResponse(responseCode = "200", description = "Sesión cerrada")
    public ResponseEntity<Void> logout(HttpServletRequest request, Authentication auth) {
        if (auth != null && auth.isAuthenticated()
                && !auth.getPrincipal().equals("anonymousUser")) {
            try {
                int idUsuario = Integer.parseInt(auth.getName());
                auditoriaService.registrarEventoManual(
                        "SISTEMA", TipoAccion.LOGOUT, idUsuario, request.getRemoteAddr());
            } catch (Exception e) {
                log.warn("No se pudo auditar el logout: {}", e.getMessage());
            }
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/olvide-contrasenia")
    @Operation(summary = "Olvidé mi contraseña",
            description = "Genera una nueva contraseña temporal y la envía al email registrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Si el email existe, se envió la contraseña")
    })
    public ResponseEntity<Void> olvideMiContrasenia(
            @RequestBody @Valid OlvideContraseniaDTO dto) {
        authService.olvideMiContrasenia(dto.getEmail());
        return ResponseEntity.ok().build();
    }
}