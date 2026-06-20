package com.fides.sienep.controller;

import com.fides.sienep.dto.ReporteResponseDTO;
import com.fides.sienep.service.ReportePdfService;
import com.fides.sienep.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Generación y exportación de reportes de seguimiento estudiantil — RF30/RF31")
public class ReporteController {

    private final ReporteService reporteService;
    private final ReportePdfService reportePdfService;

    @GetMapping
    @Operation(
            summary = "Generar reporte de seguimiento",
            description = "Genera métricas agregadas de instancias, incidencias y recordatorios. " +
                    "Todos los filtros son opcionales y combinables. " +
                    "Sin filtros devuelve métricas globales. " +
                    "Requiere VER_REPORTES.")
    @ApiResponse(responseCode = "200", description = "Reporte generado exitosamente")
    @ApiResponse(responseCode = "403", description = "Sin permiso VER_REPORTES")
    public ResponseEntity<ReporteResponseDTO> generarReporte(
            @RequestParam(required = false) Integer idCarrera,
            @RequestParam(required = false) Integer idGrupo,
            @RequestParam(required = false) Integer idEstudiante,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        return ResponseEntity.ok(
                reporteService.generarReporte(idCarrera, idGrupo, idEstudiante, desde, hasta));
    }

    @GetMapping("/pdf")
    @Operation(
            summary = "Exportar reporte en PDF",
            description = "Genera y descarga el reporte en formato PDF con los mismos filtros disponibles. " +
                    "Requiere EXPORTAR_REPORTES.")
    @ApiResponse(responseCode = "200", description = "PDF generado exitosamente")
    @ApiResponse(responseCode = "403", description = "Sin permiso EXPORTAR_REPORTES")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(required = false) Integer idCarrera,
            @RequestParam(required = false) Integer idGrupo,
            @RequestParam(required = false) Integer idEstudiante,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        ReporteResponseDTO reporte = reporteService.generarReporte(
                idCarrera, idGrupo, idEstudiante, desde, hasta);

        byte[] pdf = reportePdfService.generarPdf(reporte);

        String nombreArchivo = "reporte_sienep_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}