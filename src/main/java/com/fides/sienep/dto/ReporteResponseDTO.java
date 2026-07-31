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

    // ── Paginación (null cuando el reporte no está paginado, ej. exportación PDF) ──
    @Schema(description = "Número de página actual (0-indexado). Null si el reporte no está paginado.")
    private Integer pagina;

    @Schema(description = "Tamaño de página solicitado. Null si el reporte no está paginado.")
    private Integer tamanioPagina;

    @Schema(description = "Total de páginas disponibles. Null si el reporte no está paginado.")
    private Integer totalPaginas;

    @Schema(description = "Total de estudiantes que matchean los filtros, a través de todas las páginas. " +
            "Null si el reporte no está paginado (en ese caso totalEstudiantes ya representa el total real).")
    private Long totalEstudiantesFiltrados;

    // ── Métricas globales ─────────────────────────────────────
    // Nota: estas métricas siempre reflejan exactamente los estudiantes de
    // detallePorEstudiante (la página actual si el reporte está paginado, o
    // el total filtrado si no lo está) — nunca un agregado "oculto" de datos
    // que no aparecen en el detalle.
    @Schema(description = "Total de estudiantes incluidos en este resultado (esta página, si el reporte está paginado)")
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