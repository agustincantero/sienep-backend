package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO que representa una Categoría de Instancia Común con su información completa.")
public class CategoriaInstanciaResponseDTO {

    @Schema(description = "ID autogenerado de la categoría", example = "1")
    private int idCategoria;

    @Schema(description = "Nombre único de la categoría de instancia", example = "EVALUACION_PSICOPEDAGOGICA")
    private String nomCategoria;

    @Schema(description = "Descripción detallada de la categoría", example = "Instancia de evaluación profunda a cargo del equipo psicopedagógico")
    private String descripcion;

    @Schema(description = "Estado actual de la categoría", example = "ACTIVO")
    private EstadoLogico estado;
}
