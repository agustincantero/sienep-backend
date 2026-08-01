package com.fides.sienep.controller;

import com.fides.sienep.dto.ReporteResponseDTO;
import com.fides.sienep.dto.ReporteValidacionResponseDTO;
import com.fides.sienep.service.ReportePdfService;
import com.fides.sienep.service.ReporteService;
import com.fides.sienep.service.ReporteVerificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Generación y exportación de reportes de seguimiento estudiantil — RF30/RF31")
public class ReporteController {

    private final ReporteService reporteService;
    private final ReportePdfService reportePdfService;
    private final ReporteVerificacionService reporteVerificacionService;

    @GetMapping
    @Operation(
            summary = "Generar reporte de seguimiento",
            description = "Genera métricas agregadas de instancias, incidencias y recordatorios. " +
                    "Todos los filtros son opcionales y combinables. " +
                    "Sin filtros devuelve métricas globales, paginadas (los totales del reporte " +
                    "reflejan únicamente los estudiantes de la página actual — ver totalEstudiantesFiltrados " +
                    "para el total real a través de todas las páginas). " +
                    "Requiere VER_REPORTES.")
    @ApiResponse(responseCode = "200", description = "Reporte generado exitosamente")
    @ApiResponse(responseCode = "403", description = "Sin permiso VER_REPORTES")
    public ResponseEntity<ReporteResponseDTO> generarReporte(
            @RequestParam(required = false) Integer idCarrera,
            @RequestParam(required = false) Integer idGrupo,
            @RequestParam(required = false) Integer idEstudiante,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @ParameterObject @PageableDefault(size = 20, sort = "apellido") Pageable pageable) {

        return ResponseEntity.ok(
                reporteService.generarReporte(idCarrera, idGrupo, idEstudiante, desde, hasta, pageable));
    }

    @GetMapping("/pdf")
    @PreAuthorize("hasAuthority('EXPORTAR_REPORTES')")
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

        // Sin Pageable a propósito: el PDF es un documento autocontenido, tiene
        // que representar el total de la población filtrada, no una página.
        ReporteResponseDTO reporte = reporteService.generarReporte(
                idCarrera, idGrupo, idEstudiante, desde, hasta, null);

        byte[] pdf = reportePdfService.generarPdf(reporte);

        reporteVerificacionService.registrarGeneracion(pdf);

        String nombreArchivo = "reporte_sienep_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping(value = "/validar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Validar autenticidad de un reporte PDF",
            description = "Endpoint público (no requiere autenticación). Sube un PDF previamente exportado " +
                    "desde SIENEP y confirma si es auténtico — es decir, si fue generado por el sistema y " +
                    "no fue alterado desde entonces — comparando el hash SHA-256 del archivo subido contra " +
                    "el hash guardado al momento de exportarlo.")
    @ApiResponse(responseCode = "200", description = "Resultado de la validación (auténtico o no)")
    @ApiResponse(responseCode = "400", description = "No se adjuntó un archivo válido")
    @ApiResponse(responseCode = "429", description = "Demasiadas validaciones desde esta IP, reintentar más tarde")
    public ResponseEntity<ReporteValidacionResponseDTO> validar(
            @RequestParam("archivo") MultipartFile archivo, HttpServletRequest request) {
        return ResponseEntity.ok(reporteVerificacionService.validar(archivo, request.getRemoteAddr()));
    }
}