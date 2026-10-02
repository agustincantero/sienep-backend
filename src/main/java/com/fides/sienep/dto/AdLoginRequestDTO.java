// dto/AdLoginRequestDTO.java
package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Credenciales de Active Directory (usuario y contraseña del dominio)")
public class AdLoginRequestDTO {

    @NotBlank
    @Schema(description = "Usuario de Active Directory (sAMAccountName), sin el dominio")
    private String usuario;

    @NotBlank
    @Schema(description = "Contraseña del dominio")
    private String contrasenia;
}
