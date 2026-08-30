package com.fides.sienep.controller;

import com.fides.sienep.dto.ReporteResponseDTO;
import com.fides.sienep.dto.ReporteValidacionResponseDTO;
import com.fides.sienep.service.ReportePdfService;
import com.fides.sienep.service.ReporteService;
import com.fides.sienep.service.ReporteVerificacionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// P1 de la auditoría — mismo criterio que CarreraControllerTest (ver ese archivo
// y AbstractControllerTest para el porqué del diseño). exportarPdf tiene
// @PreAuthorize a nivel de CONTROLLER (única excepción del proyecto, ver
// auditoría sección 1) — igual no se ejerce acá porque SecurityConfig
// (@EnableMethodSecurity) no se carga en este slice; probarlo de verdad
// requeriría el patrón @SpringBootTest de los tests de comentarios confidenciales.
@WebMvcTest(ReporteController.class)
class ReporteControllerTest extends AbstractControllerTest {

    @MockitoBean private ReporteService reporteService;
    @MockitoBean private ReportePdfService reportePdfService;
    @MockitoBean private ReporteVerificacionService reporteVerificacionService;

    private ReporteResponseDTO responseDTO() {
        return ReporteResponseDTO.builder()
                .fechaGeneracion(LocalDateTime.now())
                .totalEstudiantes(10)
                .build();
    }

    @Test
    @DisplayName("Generar reporte - Sin filtros responde 200")
    void generarReporte_SinFiltros_Responde200() throws Exception {
        when(reporteService.generarReporte(any(), any(), any(), any(), any(), any())).thenReturn(responseDTO());

        mockMvc.perform(get("/reportes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEstudiantes").value(10));
    }

    @Test
    @DisplayName("Exportar PDF - Responde 200 con el PDF y el nombre de archivo correcto")
    void exportarPdf_Responde200() throws Exception {
        when(reporteService.generarReporte(any(), any(), any(), any(), any(), any())).thenReturn(responseDTO());
        when(reportePdfService.generarPdf(any())).thenReturn("pdf-fake".getBytes());

        mockMvc.perform(get("/reportes/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @DisplayName("Validar - Archivo válido responde 200")
    void validar_ArchivoValido_Responde200() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "reporte.pdf", "application/pdf", "contenido".getBytes());
        when(reporteVerificacionService.validar(any(), any()))
                .thenReturn(ReporteValidacionResponseDTO.builder().autentico(true).build());

        mockMvc.perform(multipart("/reportes/validar").file(archivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autentico").value(true));
    }

    @Test
    @DisplayName("Validar - Demasiadas validaciones desde la IP responde 429 (propagado desde el service)")
    void validar_LimiteExcedido_Responde429() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "reporte.pdf", "application/pdf", "contenido".getBytes());
        when(reporteVerificacionService.validar(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas validaciones"));

        mockMvc.perform(multipart("/reportes/validar").file(archivo))
                .andExpect(status().isTooManyRequests());
    }
}
