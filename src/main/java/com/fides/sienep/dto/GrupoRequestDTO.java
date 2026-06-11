package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para crear o actualizar un grupo")
public class GrupoRequestDTO {

    @NotBlank
    @Size(max = 60)
    @Schema(description = "Nombre del grupo", example = "Grupo A - Turno Mañana")
    private String nomGrupo;

    @NotNull
    @Schema(description = "ID de la carrera a la que pertenece el grupo", example = "1")
    private Integer idCarrera;

    @NotNull
    @Schema(description = "Año de generación del grupo", example = "2024")
    private Integer generacion;
}