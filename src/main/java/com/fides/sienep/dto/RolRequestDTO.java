package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Objeto de transferencia de datos para la creación o actualización de un Rol")
public class RolRequestDTO {

    @Schema(description = "Nombre del rol (debe ser único)", example = "COORDINADOR")
    @NotBlank(message = "El nombre del rol no puede estar vacío")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
    private String nombre;

    @Schema(description = "Descripción detallada del propósito del rol", example = "Encargado de coordinar las actividades académicas")
    @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
    private String descripcion;

    @Schema(description = "Estado lógico actual del rol", example = "ACTIVO")
    @NotNull(message = "El estado no puede ser nulo")
    private EstadoLogico estado;

    @Schema(description = "Lista de IDs de los permisos que se desean asignar al rol", example = "[1, 2, 5]")
    private List<Integer> permisosIds;
}
