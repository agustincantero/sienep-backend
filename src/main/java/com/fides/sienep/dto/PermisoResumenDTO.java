package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Representación resumida de un permiso para incluir dentro de otros objetos")
public class PermisoResumenDTO {
    
    @Schema(description = "ID único del permiso", example = "1")
    private int idPermiso;
    
    @Schema(description = "Nombre clave del permiso", example = "VER_FUNCIONARIOS")
    private String nombre;
}
