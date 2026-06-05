package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Schema(description = "Datos de respuesta de un Permiso")
public class PermisoResponseDTO {
    @Schema(description = "ID único del permiso", example = "1")
    private int idPermiso;

    @Schema(description = "Nombre inmutable del permiso", example = "VER_USUARIO")
    private String nombre;

    @Schema(description = "Descripción detallada", example = "Permite visualizar usuarios")
    private String descripcion;

    @Schema(description = "Estado lógico actual", example = "ACTIVO")
    private EstadoLogico estado;
}
