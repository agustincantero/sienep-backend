package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Métricas de seguimiento de un estudiante individual")
public class ReporteEstudianteDTO {

    private Integer idEstudiante;
    private String nombreCompleto;
    private String documento;

    @Schema(description = "Cantidad de instancias registradas en el período")
    private Integer cantidadInstancias;

    @Schema(description = "Cantidad de incidencias registradas en el período")
    private Integer cantidadIncidencias;

    @Schema(description = "Cantidad de recordatorios asociados a sus instancias")
    private Integer cantidadRecordatorios;
}