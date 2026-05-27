package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "DTO para la actualización total (PUT) de un Rol.")
public class RolUpdateDTO {

    @Schema(description = "Nombre único del rol", example = "COORDINADOR_GENERAL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El nombre del rol no puede estar vacío")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
    private String nombre;

    @Schema(description = "Descripción detallada del rol", example = "Coordinador general", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
    private String descripcion;

    @Schema(description = "Estado actual del rol (ACTIVO, INACTIVO, etc.)", example = "ACTIVO", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El estado no puede ser nulo en una actualización total")
    private EstadoLogico estado;

    @Schema(description = "Lista completa de IDs de permisos que debe tener el rol. Una lista vacía [] elimina todos sus permisos.", example = "[1, 3]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<Integer> permisosIds;
}
