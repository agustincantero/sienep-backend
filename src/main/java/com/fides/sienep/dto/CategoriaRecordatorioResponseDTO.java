package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO que representa una Categoría de Recordatorio con su información completa.")
public class CategoriaRecordatorioResponseDTO {

    @Schema(description = "ID autogenerado de la categoría", example = "1")
    private int idCategoria;

    @Schema(description = "Nombre único de la categoría de recordatorio", example = "FALTA_INJUSTIFICADA")
    private String nomCategoria;

    @Schema(description = "Descripción detallada de la categoría", example = "Alerta generada por ausencias reiteradas del estudiante sin justificar")
    private String descripcion;

    @Schema(description = "Estado actual de la categoría", example = "ACTIVO")
    private EstadoLogico estado;
}
