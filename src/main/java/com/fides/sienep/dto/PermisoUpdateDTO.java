package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO para editar la descripción de un permiso. El nombre y el estado son inmutables.")
public class PermisoUpdateDTO {

    @Schema(description = "Nueva descripción aclaratoria del permiso", example = "Permite a los usuarios acceder al panel de instancias")
    @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
    private String descripcion;
}
