package com.fides.sienep.controller;

import com.fides.sienep.dto.ReporteResponseDTO;
import com.fides.sienep.dto.ReporteValidacionResponseDTO;
import com.fides.sienep.service.ReportePdfService;
import com.fides.sienep.service.ReporteService;
import com.fides.sienep.service.ReporteVerificacionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// P1 de la auditoría — mismo criterio que CarreraControllerTest (ver ese archivo
// y AbstractControllerTest para el porqué del diseño).
@WebMvcTest(ReporteController.class)
class ReporteControllerTest extends AbstractControllerTest {

    @MockitoBean private ReporteService reporteService;
    @MockitoBean private ReportePdfService reportePdfService;
    @MockitoBean private ReporteVerificacionService reporteVerificacionService;

    private ReporteResponseDTO responseDTO() {
        return ReporteResponseDTO.builder()
                .fechaGeneracion(LocalDateTime.now())
                .totalInstancias(10)
                .build();
    }

    @Test
    @DisplayName("Generar reporte - Sin filtros responde 200")
    void generarReporte_SinFiltros_Responde200() throws Exception {
        when(reporteService.generarReporte(any(), any())).thenReturn(responseDTO());

        mockMvc.perform(get("/reportes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInstancias").value(10));
    }

    @Test
    @DisplayName("Generar reporte - Pasa desde y hasta al service")
    void generarReporte_ConPeriodo_PasaLasFechasAlService() throws Exception {
        when(reporteService.generarReporte(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 9, 30))).thenReturn(responseDTO());

        mockMvc.perform(get("/reportes").param("desde", "2026-03-01").param("hasta", "2026-09-30"))
                .andExpect(status().isOk());

        verify(reporteService).generarReporte(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 9, 30));
    }

    @Test
    @DisplayName("Generar reporte - Fecha con formato inválido responde 400")
    void generarReporte_FechaInvalida_Responde400() throws Exception {
        mockMvc.perform(get("/reportes").param("desde", "01/03/2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Exportar PDF - Responde 200 con el PDF y el nombre de archivo correcto")
    void exportarPdf_Responde200() throws Exception {
        when(reporteService.generarReporte(any(), any())).thenReturn(responseDTO());
        when(reportePdfService.generarPdf(any())).thenReturn("pdf-fake".getBytes());

        mockMvc.perform(get("/reportes/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        allOf(startsWith("attachment;"), containsString("filename*=UTF-8''reporte_sienep_"))));
    }

    @Test
    @DisplayName("Validar - Archivo válido responde 200")
    void validar_ArchivoValido_Responde200() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "reporte.pdf", "application/pdf", "contenido".getBytes());
        when(reporteVerificacionService.validar(any()))
                .thenReturn(ReporteValidacionResponseDTO.builder().autentico(true).build());

        mockMvc.perform(multipart("/reportes/validar").file(archivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autentico").value(true));
    }
}
