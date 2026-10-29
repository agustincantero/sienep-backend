package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "Reporte de seguimiento con los indicadores IND-01 a IND-07 leídos del Data Warehouse, por mes completo")
public class ReporteResponseDTO {

    // ── Metadata del documento ────────────────────────────────
    @Schema(description = "Fecha y hora de generación del reporte")
    private LocalDateTime fechaGeneracion;

    @Schema(description = "Usuario que generó el reporte")
    private String generadoPor;

    @Schema(description = "Fecha de la última carga correcta del Data Warehouse, que es hasta cuando llegan los datos. Null si todavía no hubo ninguna")
    private LocalDateTime datosAl;

    // ── Período del reporte ───────────────────────────────────
    @Schema(description = "Primer día del primer mes analizado. Null si el Data Warehouse no tiene datos")
    private LocalDate desde;

    @Schema(description = "Último día del último mes analizado (inclusive). Null si el Data Warehouse no tiene datos")
    private LocalDate hasta;

    // ── Totales del período ───────────────────────────────────
    @Schema(description = "Total de instancias comunes activas agendadas en el período")
    private Integer totalInstancias;

    @Schema(description = "Total de incidencias activas ocurridas en el período")
    private Integer totalIncidencias;

    // ── Indicadores (mismos nombres de columna que las vistas v_* del DW) ──
    @Schema(description = "IND-01 e IND-02. Instancias agendadas e incidencias ocurridas por mes")
    private List<EvolucionMensual> evolucionMensual;

    @Schema(description = "IND-03. Instancias por categoría y mes, con el peso de cada categoría dentro del mes")
    private List<InstanciasCategoriaMes> instanciasPorCategoria;

    @Schema(description = "IND-04. Instancias por canal y mes, con el peso de cada canal dentro del mes")
    private List<InstanciasCanalMes> instanciasPorCanal;

    @Schema(description = "IND-05. Incidencias por lugar y mes")
    private List<IncidenciasLugarMes> incidenciasPorLugar;

    @Schema(description = "IND-06. Incidencias por día de la semana y franja horaria, por mes")
    private List<IncidenciasDiaFranja> incidenciasPorDiaFranja;

    @Schema(description = "IND-07. Involucrados registrados por incidencia, por mes")
    private List<IncidenciasInvolucradosMes> incidenciasInvolucrados;

    // ── Filas de cada indicador ───────────────────────────────
    // mes es el primer día del mes y anioMes su forma AAAA-MM; solo aparecen los meses con datos
    @Getter
    @AllArgsConstructor
    public static class EvolucionMensual {
        private final LocalDate mes;
        private final String anioMes;
        private final int instanciasAgendadas;
        private final int incidenciasOcurridas;
    }

    @Getter
    @AllArgsConstructor
    public static class InstanciasCategoriaMes {
        private final LocalDate mes;
        private final String anioMes;
        private final String categoria;
        private final int instancias;
        private final BigDecimal porcentajeDelMes;
    }

    @Getter
    @AllArgsConstructor
    public static class InstanciasCanalMes {
        private final LocalDate mes;
        private final String anioMes;
        private final String canal;
        private final String modalidad;
        private final int instancias;
        private final BigDecimal porcentajeDelMes;
    }

    @Getter
    @AllArgsConstructor
    public static class IncidenciasLugarMes {
        private final LocalDate mes;
        private final String anioMes;
        private final String tipoEspacio;
        private final String lugar;
        private final int incidencias;
    }

    @Getter
    @AllArgsConstructor
    public static class IncidenciasDiaFranja {
        private final LocalDate mes;
        private final String anioMes;
        private final int diaSemanaNro;
        private final String diaSemana;
        private final String franjaHoraria;
        private final int incidencias;
    }

    // Con 0 involucrados puede ser "ninguno" o "todavía no cargados"
    @Getter
    @AllArgsConstructor
    public static class IncidenciasInvolucradosMes {
        private final LocalDate mes;
        private final String anioMes;
        private final int incidencias;
        private final int totalInvolucrados;
        private final int incidenciasConMasDeUno;
        private final BigDecimal promedioInvolucrados;
        private final BigDecimal porcentajeConMasDeUno;
    }
}
