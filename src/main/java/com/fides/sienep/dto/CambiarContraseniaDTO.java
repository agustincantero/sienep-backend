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
    @Schema(description = "Contraseña actual, para confirmar la identidad antes de cambiarla", example = "ViejaPass123!")
    private String contraseniaActual;

    @NotBlank
    @Size(min = 8, max = 100)
    @Schema(description = "Nueva contraseña", example = "NuevaPass123!")
    private String contrasenia;
}