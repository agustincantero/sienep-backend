package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para cambiar la contraseña del usuario")
public class CambiarContraseniaDTO {

    @NotBlank
    @Size(min = 8, max = 100)
    @Schema(description = "Nueva contraseña", example = "NuevaPass123!")
    private String contrasenia;
}