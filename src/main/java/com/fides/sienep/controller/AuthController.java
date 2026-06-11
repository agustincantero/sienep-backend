package com.fides.sienep.controller;

import com.fides.sienep.dto.LoginRequestDTO;
import com.fides.sienep.dto.LoginResponseDTO;
import com.fides.sienep.model.Funcionario;
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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login y logout del sistema")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final FuncionarioRepository funcionarioRepository;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
            description = "Autentica al funcionario y devuelve un token JWT válido por 5 horas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {

        // 1. Buscar funcionario por email para obtener ID y rol
        Funcionario funcionario = funcionarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        // 2. Validar email + contraseña (usa LoginUsuarioDetailsService internamente)
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.getEmail(), dto.getContrasenia()));
        } catch (BadCredentialsException e) {
            log.warn("Login fallido para id: {}", funcionario.getIdUsuario());
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        // 3. Generar token — el rol viene directo del funcionario, sin query extra
        String rol = funcionario.getRol().getNombre();
        String token = jwtTokenUtil.generateToken(funcionario.getIdUsuario(), rol);
        log.info("Login exitoso: id={} rol={}", funcionario.getIdUsuario(), rol);
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión",
            description = "Stateless: el cliente descarta el token.")
    @ApiResponse(responseCode = "200", description = "Sesión cerrada")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }
}