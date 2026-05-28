package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "DTO para la creación de un nuevo Rol. No requiere estado ni ID.")
public class RolCreateDTO {

    @Schema(description = "Nombre único del rol", example = "COORDINADOR", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El nombre del rol no puede estar vacío")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
    private String nombre;

    @Schema(description = "Descripción detallada del rol", example = "Encargado de coordinar actividades estudiantiles", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
    private String descripcion;

    @Schema(description = "Lista de IDs de los permisos que se desean asignar al nuevo rol", example = "[1, 2, 5]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<Integer> permisosIds;
}
