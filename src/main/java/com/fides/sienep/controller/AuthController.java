package com.fides.sienep.controller;

import com.fides.sienep.dto.LoginRequestDTO;
import com.fides.sienep.dto.LoginResponseDTO;
import com.fides.sienep.dto.OlvideContraseniaDTO;
import com.fides.sienep.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login, logout y recuperación de contraseña")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
            description = "Autentica un funcionario o estudiante y devuelve un token JWT válido por 5 horas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
            @ApiResponse(responseCode = "403", description = "Cuenta bloqueada")
    })
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto, HttpServletRequest request) {
        LoginResponseDTO response = authService.login(dto, request.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión",
            description = "Stateless: el cliente descarta el token.")
    @ApiResponse(responseCode = "200", description = "Sesión cerrada")
    public ResponseEntity<Void> logout(HttpServletRequest request, Authentication auth) {
        authService.logout(auth, request.getRemoteAddr());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/olvide-contrasenia")
    @Operation(summary = "Olvidé mi contraseña",
            description = "Genera una nueva contraseña temporal y la envía al email registrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Si el email existe, se envió la contraseña"),
            @ApiResponse(responseCode = "400", description = "Body malformado o email con formato inválido")
    })
    public ResponseEntity<Void> olvideMiContrasenia(@RequestBody @Valid OlvideContraseniaDTO dto) {
        authService.olvideMiContrasenia(dto.getEmail());
        return ResponseEntity.ok().build();
    }
}