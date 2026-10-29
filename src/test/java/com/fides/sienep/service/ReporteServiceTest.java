package com.fides.sienep.service;

import com.fides.sienep.dto.ReporteResponseDTO;
import com.fides.sienep.dto.ReporteResponseDTO.EvolucionMensual;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.config.DwJdbc;
import com.fides.sienep.repository.ReporteDwRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    private static final LocalDate MARZO = LocalDate.of(2026, 3, 1);
    private static final LocalDate ABRIL = LocalDate.of(2026, 4, 1);

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private ReporteDwRepository reporteDwRepository;

    @InjectMocks
    private ReporteService reporteService;

    @BeforeEach
    void setUp() {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("1");
        lenient().doReturn(new ArrayList<>()).when(auth).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(auth);

        Funcionario funcionarioAutenticadoMock = new Funcionario();
        funcionarioAutenticadoMock.setNombre("Mariana");
        funcionarioAutenticadoMock.setApellido("Fernández");
        funcionarioAutenticadoMock.setDocumento("11222333");
        lenient().when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioAutenticadoMock));

        lenient().when(reporteDwRepository.enSnapshot(any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        lenient().when(reporteDwRepository.evolucionMensual(any(), any())).thenReturn(List.of());
        lenient().when(reporteDwRepository.instanciasPorCategoria(any(), any())).thenReturn(List.of());
        lenient().when(reporteDwRepository.instanciasPorCanal(any(), any())).thenReturn(List.of());
        lenient().when(reporteDwRepository.incidenciasPorLugar(any(), any())).thenReturn(List.of());
        lenient().when(reporteDwRepository.incidenciasPorDiaFranja(any(), any())).thenReturn(List.of());
        lenient().when(reporteDwRepository.incidenciasInvolucrados(any(), any())).thenReturn(List.of());
        lenient().when(reporteDwRepository.ultimaActualizacion()).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================
    // METADATOS Y SIN DATOS
    // =========================================================

    @Test
    @DisplayName("Generar reporte - DW sin datos devuelve totales en 0, listas vacías y período nulo")
    void generarReporte_SinDatos_DevuelveVacio() {
        ReporteResponseDTO resultado = reporteService.generarReporte(null, null);

        assertEquals(0, resultado.getTotalInstancias());
        assertEquals(0, resultado.getTotalIncidencias());
        assertTrue(resultado.getEvolucionMensual().isEmpty());
        assertNull(resultado.getDesde());
        assertNull(resultado.getHasta());
        assertNull(resultado.getDatosAl());
        assertNotNull(resultado.getFechaGeneracion());
        assertEquals("Mariana Fernández · 11222333", resultado.getGeneradoPor());
    }

    @Test
    @DisplayName("Generar reporte - Informa hasta cuándo llegan los datos del DW")
    void generarReporte_InformaDatosAl() {
        LocalDateTime ultimaCarga = LocalDateTime.of(2026, 10, 24, 22, 6);
        when(reporteDwRepository.ultimaActualizacion()).thenReturn(ultimaCarga);

        assertEquals(ultimaCarga, reporteService.generarReporte(null, null).getDatosAl());
    }

    @Test
    @DisplayName("Generar reporte - Funcionario autenticado no encontrado devuelve 404")
    void generarReporte_FuncionarioAutenticadoNoEncontrado_Lanza404() {
        when(funcionarioRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reporteService.generarReporte(null, null));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // =========================================================
    // PERÍODO (POR MES COMPLETO)
    // =========================================================

    @Test
    @DisplayName("Generar reporte - Fecha desde posterior a hasta devuelve 400 sin consultar el DW")
    void generarReporte_FechasInvertidas_Lanza400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reporteService.generarReporte(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 3, 1)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(reporteDwRepository);
    }

    @Test
    @DisplayName("Generar reporte - Redondea desde y hasta a los meses que tocan")
    void generarReporte_RedondeaElPeriodoAMeses() {
        ReporteResponseDTO resultado = reporteService.generarReporte(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 5, 20));

        // Las vistas se consultan por el primer día de cada mes
        verify(reporteDwRepository).evolucionMensual(MARZO, LocalDate.of(2026, 5, 1));
        verify(reporteDwRepository).instanciasPorCanal(MARZO, LocalDate.of(2026, 5, 1));
        // El período informado son meses completos
        assertEquals(MARZO, resultado.getDesde());
        assertEquals(LocalDate.of(2026, 5, 31), resultado.getHasta());
    }

    @Test
    @DisplayName("Generar reporte - Sin fechas consulta sin límite y toma el período de lo cargado")
    void generarReporte_SinFechas_TomaTodoLoCargado() {
        when(reporteDwRepository.evolucionMensual(null, null)).thenReturn(List.of(
                new EvolucionMensual(MARZO, "2026-03", 10, 2),
                new EvolucionMensual(ABRIL, "2026-04", 5, 1),
                new EvolucionMensual(LocalDate.of(2026, 9, 1), "2026-09", 7, 3)));

        ReporteResponseDTO resultado = reporteService.generarReporte(null, null);

        verify(reporteDwRepository).instanciasPorCategoria(null, null);
        assertEquals(MARZO, resultado.getDesde());
        assertEquals(LocalDate.of(2026, 9, 30), resultado.getHasta());
    }

    @Test
    @DisplayName("Generar reporte - Febrero de un año bisiesto termina el 29")
    void generarReporte_UltimoDiaDelMes() {
        ReporteResponseDTO resultado = reporteService.generarReporte(LocalDate.of(2028, 1, 10), LocalDate.of(2028, 2, 3));

        assertEquals(LocalDate.of(2028, 2, 29), resultado.getHasta());
    }

    @Test
    @DisplayName("Generar reporte - Desde sin hasta y sin datos en el rango informa sin período, no a medias")
    void generarReporte_DesdeSinHastaYSinDatos_PeriodoNulo() {
        ReporteResponseDTO resultado = reporteService.generarReporte(LocalDate.of(2030, 1, 15), null);

        assertNull(resultado.getDesde());
        assertNull(resultado.getHasta());
    }

    @Test
    @DisplayName("Generar reporte - Lee todas las vistas dentro de un mismo snapshot del DW")
    void generarReporte_LeeEnUnSnapshot() {
        reporteService.generarReporte(null, null);

        verify(reporteDwRepository, times(1)).enSnapshot(any());
    }

    // =========================================================
    // TOTALES Y PASE DE LAS VISTAS
    // =========================================================

    @Test
    @DisplayName("Generar reporte - Los totales son la suma de la serie mensual")
    void generarReporte_TotalesSumanLaSerie() {
        when(reporteDwRepository.evolucionMensual(any(), any())).thenReturn(List.of(
                new EvolucionMensual(MARZO, "2026-03", 154, 20),
                new EvolucionMensual(ABRIL, "2026-04", 153, 19)));

        ReporteResponseDTO resultado = reporteService.generarReporte(MARZO, ABRIL);

        assertEquals(307, resultado.getTotalInstancias());
        assertEquals(39, resultado.getTotalIncidencias());
        assertEquals(2, resultado.getEvolucionMensual().size());
    }

    @Test
    @DisplayName("Generar reporte - Devuelve las filas de cada vista tal como vienen del DW, sin recalcular")
    void generarReporte_PasaLasVistasSinTransformar() {
        var porCategoria = List.of(new ReporteResponseDTO.InstanciasCategoriaMes(
                MARZO, "2026-03", "COORDINACIÓN", 55, new java.math.BigDecimal("35.7")));
        var porCanal = List.of(new ReporteResponseDTO.InstanciasCanalMes(
                MARZO, "2026-03", "Google Meet", "A distancia", 44, new java.math.BigDecimal("28.6")));
        when(reporteDwRepository.instanciasPorCategoria(any(), any())).thenReturn(porCategoria);
        when(reporteDwRepository.instanciasPorCanal(any(), any())).thenReturn(porCanal);

        ReporteResponseDTO resultado = reporteService.generarReporte(MARZO, MARZO);

        assertSame(porCategoria, resultado.getInstanciasPorCategoria());
        assertSame(porCanal, resultado.getInstanciasPorCanal());
    }

    // =========================================================
    // DW NO DISPONIBLE
    // =========================================================

    @Test
    @DisplayName("Generar reporte - Si el DW no está disponible propaga el 503")
    void generarReporte_DwNoDisponible_Propaga503() {
        when(reporteDwRepository.evolucionMensual(any(), any()))
                .thenThrow(DwJdbc.noDisponible("El Data Warehouse no está configurado", null));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reporteService.generarReporte(null, null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }
}
