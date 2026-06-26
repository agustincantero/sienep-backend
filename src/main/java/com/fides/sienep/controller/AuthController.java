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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.fides.sienep.service.GoogleOAuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login, logout y recuperación de contraseña")
public class AuthController {

    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;

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

    // ── Iniciar flow OAuth2 ───────────────────────────────────────────────────
// El funcionario autenticado hace GET a este endpoint.
// El backend genera la URL de Google y redirige al navegador allá.
// Google pedirá que el usuario acepte los permisos de Calendar.

    @Operation(summary = "Iniciar conexión con Google Calendar",
            description = "Redirige al funcionario autenticado al flow de autorización de Google")
    @ApiResponse(responseCode = "302", description = "Redirección a Google")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    @GetMapping("/google/init")
    @PreAuthorize("hasAuthority('GCAL_SYNC_RECORDATORIOS')")
    public ResponseEntity<Void> iniciarOAuth() {
        int idUsuario = Integer.parseInt(
                SecurityContextHolder.getContext().getAuthentication().getName());
        String url = googleOAuthService.generarUrlAutorizacion(idUsuario);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

// ── Callback OAuth2 ───────────────────────────────────────────────────────
// Google redirige aquí después de que el usuario acepta (o rechaza).
// No requiere JWT porque el navegador del usuario llega aquí directamente
// desde Google — no hay forma de que traiga el token de SIENEP.
// La seguridad está en el parámetro `state` (idUsuario firmado implícitamente
// por haber sido generado en /google/init con el JWT del usuario).

    @Operation(summary = "Callback OAuth2 de Google Calendar",
            description = "Endpoint al que Google redirige tras la autorización del usuario")
    @ApiResponse(responseCode = "200", description = "Cuenta de Google conectada correctamente")
    @ApiResponse(responseCode = "400", description = "Código de autorización inválido")
    @ApiResponse(responseCode = "502", description = "Error al comunicarse con Google")
    @GetMapping("/google/callback")
    public ResponseEntity<String> callbackOAuth(
            @RequestParam String code,
            @RequestParam String state) {
        googleOAuthService.procesarCallback(code, state);
        return ResponseEntity.ok("¡Tu cuenta de Google Calendar fue conectada correctamente! " +
                "Los próximos recordatorios aparecerán en tu calendario.");
    }
}