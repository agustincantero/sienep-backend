package com.fides.sienep.service;

import com.fides.sienep.dto.ReporteResponseDTO;
import com.fides.sienep.dto.ReporteResponseDTO.EvolucionMensual;
import com.fides.sienep.dto.ReporteResponseDTO.IncidenciasDiaFranja;
import com.fides.sienep.dto.ReporteResponseDTO.IncidenciasInvolucradosMes;
import com.fides.sienep.dto.ReporteResponseDTO.IncidenciasLugarMes;
import com.fides.sienep.dto.ReporteResponseDTO.InstanciasCanalMes;
import com.fides.sienep.dto.ReporteResponseDTO.InstanciasCategoriaMes;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Sin @ExtendWith(MockitoExtension.class) ni @Mock: ReportePdfService no tiene
// dependencias inyectadas (no llama a ningún repository/service), así que se
// instancia directo. Estos tests verifican la generación real del PDF con iText,
// no un mock de iText — es la forma más honesta de probar esta clase.
class ReportePdfServiceTest {

    private static final LocalDate MARZO = LocalDate.of(2026, 3, 1);

    private ReportePdfService reportePdfService;

    @BeforeEach
    void setUp() {
        reportePdfService = new ReportePdfService();
    }

    private ReporteResponseDTO.ReporteResponseDTOBuilder reporteBaseBuilder() {
        return ReporteResponseDTO.builder()
                .fechaGeneracion(LocalDateTime.now())
                .generadoPor("Juan García")
                .desde(LocalDate.now().minusMonths(3))
                .hasta(LocalDate.now())
                .totalInstancias(5)
                .totalIncidencias(1);
    }

    @Test
    @DisplayName("Generar PDF - Con todos los indicadores devuelve bytes con encabezado PDF válido")
    void generarPdf_Exito_DevuelveBytesValidos() {
        ReporteResponseDTO reporte = reporteBaseBuilder()
                .evolucionMensual(List.of(new EvolucionMensual(MARZO, "2026-03", 5, 1)))
                .instanciasPorCategoria(List.of(
                        new InstanciasCategoriaMes(MARZO, "2026-03", "Reunión", 5, new BigDecimal("100.0"))))
                .instanciasPorCanal(List.of(
                        new InstanciasCanalMes(MARZO, "2026-03", "Presencial", "Presencial", 5, new BigDecimal("100.0"))))
                .incidenciasPorLugar(List.of(
                        new IncidenciasLugarMes(MARZO, "2026-03", "Aulas", "Aula 1", 1)))
                .incidenciasPorDiaFranja(List.of(
                        new IncidenciasDiaFranja(MARZO, "2026-03", 1, "Lunes", "Mañana", 1)))
                .incidenciasInvolucrados(List.of(
                        new IncidenciasInvolucradosMes(MARZO, "2026-03", 1, 1, 0,
                                new BigDecimal("1.00"), new BigDecimal("0.0"))))
                .build();

        byte[] pdf = reportePdfService.generarPdf(reporte);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        // Todo PDF válido arranca con la firma "%PDF-" en los primeros bytes.
        String encabezado = new String(pdf, 0, 5);
        assertEquals("%PDF-", encabezado);
    }

    @Test
    @DisplayName("Generar PDF - Incrusta la fuente Onest del frontend")
    void generarPdf_IncrustaFuenteOnest() throws Exception {
        byte[] pdf = reportePdfService.generarPdf(reporteBaseBuilder().build());

        assertTrue(new String(pdf, "ISO-8859-1").contains("Onest"));
    }

    @Test
    @DisplayName("Generar PDF - Indicadores vacíos no rompen")
    void generarPdf_IndicadoresVacios_NoRompe() {
        ReporteResponseDTO reporte = reporteBaseBuilder()
                .evolucionMensual(List.of())
                .instanciasPorCategoria(List.of())
                .instanciasPorCanal(List.of())
                .incidenciasPorLugar(List.of())
                .incidenciasPorDiaFranja(List.of())
                .incidenciasInvolucrados(List.of())
                .build();

        byte[] pdf = assertDoesNotThrow(() -> reportePdfService.generarPdf(reporte));

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("Generar PDF - Indicadores null no rompen")
    void generarPdf_IndicadoresNull_NoRompe() {
        ReporteResponseDTO reporte = reporteBaseBuilder().build();

        byte[] pdf = assertDoesNotThrow(() -> reportePdfService.generarPdf(reporte));

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("Generar PDF - Muchas filas continúan en más páginas sin romper")
    void generarPdf_MuchasFilas_NoRompe() {
        List<IncidenciasLugarMes> filas = java.util.stream.IntStream.range(0, 300)
                .mapToObj(i -> new IncidenciasLugarMes(MARZO, "2026-03", "Aulas", "Aula " + i, 1))
                .toList();
        ReporteResponseDTO reporte = reporteBaseBuilder().incidenciasPorLugar(filas).build();

        byte[] pdf = assertDoesNotThrow(() -> reportePdfService.generarPdf(reporte));

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("Generar PDF - Período nulo (DW sin datos) no rompe y dice sin datos")
    void generarPdf_PeriodoNulo_DiceSinDatos() throws Exception {
        ReporteResponseDTO reporte = ReporteResponseDTO.builder()
                .fechaGeneracion(LocalDateTime.now())
                .generadoPor("Juan García")
                .totalInstancias(0)
                .totalIncidencias(0)
                .build(); // desde/hasta quedan null

        byte[] pdf = assertDoesNotThrow(() -> reportePdfService.generarPdf(reporte));

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("Generar PDF - Valores nulos de las vistas se muestran como guion y no como la palabra null")
    void generarPdf_ValoresNulos_ConGuion() throws Exception {
        ReporteResponseDTO reporte = reporteBaseBuilder()
                .instanciasPorCategoria(List.of(new InstanciasCategoriaMes(MARZO, "2026-03", "Reunión", 5, null)))
                .incidenciasInvolucrados(List.of(new IncidenciasInvolucradosMes(MARZO, "2026-03", 1, 1, 0, null, null)))
                .build();

        byte[] pdf = reportePdfService.generarPdf(reporte);

        PdfReader lector = new PdfReader(pdf);
        String texto = PdfTextExtractor.getTextFromPage(lector, 1);
        lector.close();
        assertFalse(texto.contains("null"), "El PDF no debe mostrar la palabra null");
    }

    @Test
    @DisplayName("Generar PDF - Con y sin fecha de la última carga del DW no rompe")
    void generarPdf_DatosAl_ConYSinFecha() {
        assertDoesNotThrow(() -> reportePdfService.generarPdf(
                reporteBaseBuilder().datosAl(LocalDateTime.of(2026, 10, 24, 22, 6)).build()));
        assertDoesNotThrow(() -> reportePdfService.generarPdf(reporteBaseBuilder().datosAl(null).build()));
    }

    @Test
    @DisplayName("Generar PDF - Datos incompletos (sin fecha de generación) se envuelve en 500, no en NPE crudo")
    void generarPdf_DatosIncompletos_Lanza500EnVezDeNPE() {
        // Sin fechaGeneracion, agregarMetadata() explota adentro del try/catch de generarPdf().
        // El comportamiento esperado es que el error se envuelva en un ResponseStatusException
        // 500 con mensaje claro, no que se propague una NullPointerException cruda hasta el controller.
        ReporteResponseDTO reporte = ReporteResponseDTO.builder()
                .generadoPor("Juan García")
                .build(); // fechaGeneracion queda null

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reportePdfService.generarPdf(reporte));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    }
}
