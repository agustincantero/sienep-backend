package com.fides.sienep.controller;

import com.fides.sienep.dto.ReporteResponseDTO;
import com.fides.sienep.dto.ReporteValidacionResponseDTO;
import com.fides.sienep.service.ReportePdfService;
import com.fides.sienep.service.ReporteService;
import com.fides.sienep.service.ReporteVerificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Generación y exportación de reportes de seguimiento estudiantil — RF30/RF31")
public class ReporteController {

    private final ReporteService reporteService;
    private final ReportePdfService reportePdfService;
    private final ReporteVerificacionService reporteVerificacionService;

    @GetMapping
    @Operation(
            summary = "Generar reporte de seguimiento",
            description = "Devuelve los indicadores IND-01 a IND-07 leídos del Data Warehouse (instancias e incidencias por mes, " +
                    "instancias por categoría y canal, incidencias por lugar, día y franja horaria, e involucrados). " +
                    "El período es por mes completo (15/03 a 20/05 trae marzo, abril y mayo) y sin fechas trae todo lo " +
                    "cargado. Es global, no admite filtros de estudiante, grupo ni carrera. El campo datosAl indica la " +
                    "última carga del Data Warehouse. Requiere VER_REPORTES.")
    @ApiResponse(responseCode = "200", description = "Reporte generado exitosamente")
    @ApiResponse(responseCode = "400", description = "La fecha 'desde' es posterior a 'hasta'")
    @ApiResponse(responseCode = "403", description = "Sin permiso VER_REPORTES")
    @ApiResponse(responseCode = "503", description = "El Data Warehouse no está configurado o no responde")
    public ResponseEntity<ReporteResponseDTO> generarReporte(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        return ResponseEntity.ok(reporteService.generarReporte(desde, hasta));
    }

    @GetMapping("/pdf")
    @Operation(
            summary = "Exportar reporte en PDF",
            description = "Genera y descarga en PDF el mismo reporte de GET /reportes. " +
                    "Requiere EXPORTAR_REPORTES.")
    @ApiResponse(responseCode = "200", description = "PDF generado exitosamente")
    @ApiResponse(responseCode = "403", description = "Sin permiso EXPORTAR_REPORTES")
    @ApiResponse(responseCode = "503", description = "El Data Warehouse no está configurado o no responde")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        ReporteResponseDTO reporte = reporteService.generarReporte(desde, hasta);

        byte[] pdf = reportePdfService.generarPdf(reporte);

        reporteVerificacionService.registrarGeneracion(pdf);

        String nombreArchivo = "reporte_sienep_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(nombreArchivo, StandardCharsets.UTF_8)
                                .build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping(value = "/validar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Validar autenticidad de un reporte PDF",
            description = "Requiere autenticación y el permiso VER_REPORTES. Sube un PDF previamente exportado " +
                    "desde SIENEP y confirma si es auténtico — es decir, si fue generado por el sistema y " +
                    "no fue alterado desde entonces — comparando el hash SHA-256 del archivo subido contra " +
                    "el hash guardado al momento de exportarlo.")
    @ApiResponse(responseCode = "200", description = "Resultado de la validación (auténtico o no)")
    @ApiResponse(responseCode = "400", description = "No se adjuntó un archivo válido")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    @ApiResponse(responseCode = "403", description = "Sin el permiso VER_REPORTES")
    public ResponseEntity<ReporteValidacionResponseDTO> validar(
            @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(reporteVerificacionService.validar(archivo));
    }
}