// dto/LoginRequestDTO.java
package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Credenciales para iniciar sesión")
public class LoginRequestDTO {

    @NotBlank
    @Email
    @Schema(description = "Email del funcionario", example = "admin@sienep.com")
    private String email;

    @NotBlank
    @Schema(description = "Contraseña", example = "Admin123!")
    private String contrasenia;
}