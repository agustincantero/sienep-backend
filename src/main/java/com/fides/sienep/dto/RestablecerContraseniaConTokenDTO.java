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
@Schema(description = "Datos para confirmar el restablecimiento de contraseña con el token recibido por email")
public class RestablecerContraseniaConTokenDTO {

    @NotBlank(message = "El token es obligatorio")
    @Schema(description = "Token recibido por email en /olvide-contrasenia", example = "3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 100)
    @Schema(description = "Nueva contraseña elegida por el usuario", example = "NuevaPass123!")
    private String contraseniaNueva;
}
