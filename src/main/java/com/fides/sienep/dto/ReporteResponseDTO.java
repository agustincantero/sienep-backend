package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "Reporte agregado de seguimiento estudiantil")
public class ReporteResponseDTO {

    // ── Metadata del documento ────────────────────────────────
    @Schema(description = "Fecha y hora de generación del reporte")
    private LocalDateTime fechaGeneracion;

    @Schema(description = "Usuario que generó el reporte")
    private String generadoPor;

    @Schema(description = "Filtros aplicados en la generación")
    private String filtrosAplicados;

    @Schema(description = "Advertencia sobre conteo cuando el filtro puede producir duplicados")
    private String advertencia;

    // ── Período del reporte ───────────────────────────────────
    @Schema(description = "Fecha de inicio del período analizado")
    private LocalDate desde;

    @Schema(description = "Fecha de fin del período analizado")
    private LocalDate hasta;

    // ── Métricas globales ─────────────────────────────────────
    @Schema(description = "Total de estudiantes incluidos en el reporte")
    private Integer totalEstudiantes;

    @Schema(description = "Total de instancias en el período")
    private Integer totalInstancias;

    @Schema(description = "Total de incidencias en el período")
    private Integer totalIncidencias;

    @Schema(description = "Total de recordatorios en el período")
    private Integer totalRecordatorios;

    @Schema(description = "Recordatorios con estado operativo VENCIDO")
    private Integer recordatoriosVencidos;

    @Schema(description = "Recordatorios con estado operativo HOY")
    private Integer recordatoriosHoy;

    @Schema(description = "Recordatorios con estado operativo FUTURO")
    private Integer recordatoriosFuturos;

    // ── Detalle por estudiante ────────────────────────────────
    @Schema(description = "Métricas individuales por cada estudiante incluido")
    private List<ReporteEstudianteDTO> detallePorEstudiante;
}