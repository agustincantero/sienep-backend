package com.fides.sienep.service;

import com.fides.sienep.dto.ReporteEstudianteDTO;
import com.fides.sienep.dto.ReporteResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Sin @ExtendWith(MockitoExtension.class) ni @Mock: ReportePdfService no tiene
// dependencias inyectadas (no llama a ningún repository/service), así que se
// instancia directo. Estos tests verifican la generación real del PDF con iText,
// no un mock de iText — es la forma más honesta de probar esta clase.
class ReportePdfServiceTest {

    private ReportePdfService reportePdfService;

    @BeforeEach
    void setUp() {
        reportePdfService = new ReportePdfService();
    }

    private ReporteResponseDTO.ReporteResponseDTOBuilder reporteBaseBuilder() {
        return ReporteResponseDTO.builder()
                .fechaGeneracion(LocalDateTime.now())
                .generadoPor("Juan García")
                .filtrosAplicados("Período: últimos 3 meses")
                .desde(LocalDate.now().minusMonths(3))
                .hasta(LocalDate.now())
                .totalEstudiantes(2)
                .totalInstancias(5)
                .totalIncidencias(1)
                .totalRecordatorios(3)
                .recordatoriosVencidos(1)
                .recordatoriosHoy(1)
                .recordatoriosFuturos(1);
    }

    @Test
    @DisplayName("Generar PDF - Éxito, devuelve bytes con encabezado PDF válido")
    void generarPdf_Exito_DevuelveBytesValidos() {
        ReporteResponseDTO reporte = reporteBaseBuilder()
                .detallePorEstudiante(List.of(
                        ReporteEstudianteDTO.builder()
                                .idEstudiante(1)
                                .nombreCompleto("Ana Pérez")
                                .documento("12345678")
                                .cantidadInstancias(3)
                                .cantidadIncidencias(1)
                                .cantidadRecordatorios(2)
                                .build()
                ))
                .build();

        byte[] pdf = reportePdfService.generarPdf(reporte);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        // Todo PDF válido arranca con la firma "%PDF-" en los primeros bytes.
        String encabezado = new String(pdf, 0, 5);
        assertEquals("%PDF-", encabezado);
    }

    @Test
    @DisplayName("Generar PDF - Detalle por estudiante vacío no rompe")
    void generarPdf_DetalleVacio_NoRompe() {
        ReporteResponseDTO reporte = reporteBaseBuilder()
                .detallePorEstudiante(List.of())
                .build();

        byte[] pdf = assertDoesNotThrow(() -> reportePdfService.generarPdf(reporte));

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("Generar PDF - Detalle por estudiante null no rompe")
    void generarPdf_DetalleNull_NoRompe() {
        ReporteResponseDTO reporte = reporteBaseBuilder()
                .detallePorEstudiante(null)
                .build();

        byte[] pdf = assertDoesNotThrow(() -> reportePdfService.generarPdf(reporte));

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("Generar PDF - Con advertencia, no rompe")
    void generarPdf_ConAdvertencia_NoRompe() {
        ReporteResponseDTO reporte = reporteBaseBuilder()
                .advertencia("El filtro por carrera puede contar instancias duplicadas si un " +
                        "estudiante cambió de carrera durante el período.")
                .detallePorEstudiante(List.of())
                .build();

        byte[] pdf = assertDoesNotThrow(() -> reportePdfService.generarPdf(reporte));

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("Generar PDF - Datos incompletos (fechas null) se envuelve en 500, no en NPE crudo")
    void generarPdf_DatosIncompletos_Lanza500EnVezDeNPE() {
        // desde/hasta en null hace que reporte.getDesde().format(...) explote adentro
        // del try/catch de generarPdf(). El comportamiento esperado es que el error se
        // envuelva en un ResponseStatusException 500 con mensaje claro, no que se
        // propague una NullPointerException cruda hasta el controller.
        ReporteResponseDTO reporte = ReporteResponseDTO.builder()
                .fechaGeneracion(LocalDateTime.now())
                .generadoPor("Juan García")
                .totalEstudiantes(0)
                .detallePorEstudiante(List.of())
                .build(); // desde/hasta quedan null

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reportePdfService.generarPdf(reporte));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    }
}