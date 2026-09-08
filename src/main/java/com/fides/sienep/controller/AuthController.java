package com.fides.sienep.controller;

import com.fides.sienep.dto.GoogleLoginRequestDTO;
import com.fides.sienep.dto.LoginRequestDTO;
import com.fides.sienep.dto.LoginResponseDTO;
import com.fides.sienep.dto.OlvideContraseniaDTO;
import com.fides.sienep.dto.RestablecerContraseniaConTokenDTO;
import com.fides.sienep.dto.UsuarioAutenticadoDTO;
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
@RequestMapping("/auth")
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

    @PostMapping("/google")
    @Operation(summary = "Iniciar sesión con Google",
            description = "Autentica con un ID token de Google (OAuth 2.0, obtenido en el frontend vía " +
                    "Google Identity Services) y devuelve un token JWT válido por 5 horas. No da de alta " +
                    "cuentas nuevas: el email que Google verificó tiene que pertenecer a un funcionario o " +
                    "estudiante ya existente en SIENEP.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso"),
            @ApiResponse(responseCode = "401", description = "ID token inválido o no existe una cuenta con ese email"),
            @ApiResponse(responseCode = "403", description = "Cuenta bloqueada")
    })
    public ResponseEntity<LoginResponseDTO> loginConGoogle(
            @RequestBody @Valid GoogleLoginRequestDTO dto, HttpServletRequest request) {
        LoginResponseDTO response = authService.loginConGoogle(dto, request.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Datos y permisos del usuario autenticado",
            description = "Devuelve identidad, tipo (FUNCIONARIO/ESTUDIANTE), rol y lista de permisos " +
                    "activos del dueño del token. Sin parámetros: siempre la cuenta propia. El frontend " +
                    "lo usa para gatear navegación y UI (shell por 'tipo', acciones por 'permisos').")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos del usuario autenticado"),
            @ApiResponse(responseCode = "401", description = "Sin token válido")
    })
    public ResponseEntity<UsuarioAutenticadoDTO> me() {
        return ResponseEntity.ok(authService.usuarioAutenticado());
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
            description = "Genera un token de un solo uso (válido 30 minutos) y manda un link al email " +
                    "registrado. La contraseña actual sigue funcionando hasta que se confirme el cambio " +
                    "en /restablecer-contrasenia con ese token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Si el email existe, se envió el link")
    })
    public ResponseEntity<Void> olvideMiContrasenia(@RequestBody @Valid OlvideContraseniaDTO dto) {
        authService.olvideMiContrasenia(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restablecer-contrasenia")
    @Operation(summary = "Confirmar restablecimiento de contraseña",
            description = "Aplica la nueva contraseña usando el token de un solo uso recibido por email " +
                    "en /olvide-contrasenia. El token vence a los 30 minutos o al primer uso.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña restablecida"),
            @ApiResponse(responseCode = "401", description = "Token inválido, vencido o ya usado")
    })
    public ResponseEntity<Void> restablecerContrasenia(@RequestBody @Valid RestablecerContraseniaConTokenDTO dto) {
        authService.restablecerContraseniaConToken(dto.getToken(), dto.getContraseniaNueva());
        return ResponseEntity.ok().build();
    }
}