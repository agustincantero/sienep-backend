package com.fides.sienep.service;

import com.fides.sienep.dto.ReporteResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.EstadoRecordatorio;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Incidencia;
import com.fides.sienep.model.InstanciaComun;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.model.Recordatorio;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.GrupoRepository;
import com.fides.sienep.repository.IncidenciaRepository;
import com.fides.sienep.repository.InstanciaComunRepository;
import com.fides.sienep.repository.RecordatorioRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private InstanciaComunRepository instanciaComunRepository;

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @Mock
    private RecordatorioRepository recordatorioRepository;

    @InjectMocks
    private ReporteService reporteService;

    private Estudiante estudianteMock;

    @BeforeEach
    void setUp() {
        estudianteMock = new Estudiante();
        estudianteMock.setNombre("Ana");
        estudianteMock.setApellido("García");
        estudianteMock.setDocumento("12345678");
        estudianteMock.setPaisDocumento(PaisDocumento.ARG);
        estudianteMock.setEmail("ana.garcia@utec.edu.uy");
        estudianteMock.setFechaNacimiento(LocalDate.of(2000, 3, 15));
        estudianteMock.setEstado(EstadoLogico.ACTIVO);
        estudianteMock.setTelefonos(new ArrayList<>());
        estudianteMock.setGrupos(new ArrayList<>());

        // Simular usuario autenticado
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("1");
        lenient().doReturn(new ArrayList<>()).when(auth).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Helper: mockear queries de métricas vacías ────────────
    private void mockearMetricasVacias(Integer idEstudiante) {
        lenient().when(instanciaComunRepository.findByEstudianteYPeriodo(
                        eq(idEstudiante), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of());
        lenient().when(incidenciaRepository.findByEstudianteYPeriodo(
                        eq(idEstudiante), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of());
        lenient().when(recordatorioRepository.findByEstudianteYPeriodo(
                        eq(idEstudiante), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of());
    }

    // ── Helper: crear recordatorio mock con estado operativo ──
    private Recordatorio buildRecordatorio(EstadoRecordatorio estadoOperativo) {
        Recordatorio r = new Recordatorio();
        r.setEstadoOperativo(estadoOperativo);
        r.setEstado(EstadoLogico.ACTIVO);
        return r;
    }

    // =========================================================
    // SIN FILTROS
    // =========================================================

    @Test
    @DisplayName("Generar reporte - Sin filtros devuelve métricas globales")
    void generarReporte_SinFiltros_DevuelveMetricasGlobales() {
        when(estudianteRepository.findAll()).thenReturn(List.of(estudianteMock));
        mockearMetricasVacias(estudianteMock.getIdUsuario());

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, null, null, null, null);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalEstudiantes());
        assertEquals(0, resultado.getTotalInstancias());
        assertEquals(0, resultado.getTotalIncidencias());
        assertEquals(0, resultado.getTotalRecordatorios());
        assertNull(resultado.getAdvertencia());
        assertNotNull(resultado.getFechaGeneracion());
        assertNotNull(resultado.getGeneradoPor());
    }

    // =========================================================
    // FILTRO POR ESTUDIANTE
    // =========================================================

    @Test
    @DisplayName("Generar reporte - Por estudiante devuelve solo ese estudiante")
    void generarReporte_PorEstudiante_DevuelveSoloEseEstudiante() {
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));
        mockearMetricasVacias(estudianteMock.getIdUsuario());

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, null, 1, null, null);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalEstudiantes());
        assertNull(resultado.getAdvertencia());
    }

    @Test
    @DisplayName("Generar reporte - Estudiante no encontrado devuelve lista vacía")
    void generarReporte_EstudianteNoEncontrado_DevuelveListaVacia() {
        when(estudianteRepository.findById(99)).thenReturn(Optional.empty());

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, null, 99, null, null);

        assertNotNull(resultado);
        assertEquals(0, resultado.getTotalEstudiantes());
        assertTrue(resultado.getDetallePorEstudiante().isEmpty());
    }

    // =========================================================
    // FILTRO POR GRUPO Y CARRERA — ADVERTENCIA
    // =========================================================

    @Test
    @DisplayName("Generar reporte - Por grupo incluye advertencia de duplicados")
    void generarReporte_PorGrupo_IncluyeAdvertencia() {
        when(estudianteRepository.findByGrupoAndEstadoList(1, EstadoLogico.ACTIVO))
                .thenReturn(List.of(estudianteMock));
        mockearMetricasVacias(estudianteMock.getIdUsuario());

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, 1, null, null, null);

        assertNotNull(resultado.getAdvertencia());
        assertTrue(resultado.getAdvertencia().contains("múltiples grupos"));
    }

    @Test
    @DisplayName("Generar reporte - Por carrera incluye advertencia de duplicados")
    void generarReporte_PorCarrera_IncluyeAdvertencia() {
        when(estudianteRepository.findByCarreraAndEstadoList(1, EstadoLogico.ACTIVO))
                .thenReturn(List.of(estudianteMock));
        mockearMetricasVacias(estudianteMock.getIdUsuario());

        ReporteResponseDTO resultado = reporteService.generarReporte(
                1, null, null, null, null);

        assertNotNull(resultado.getAdvertencia());
        assertTrue(resultado.getAdvertencia().contains("múltiples grupos"));
    }

    // =========================================================
    // MÉTRICAS
    // =========================================================

    @Test
    @DisplayName("Generar reporte - Conteo de instancias e incidencias correcto")
    void generarReporte_ConteoDeMetricasCorrecto() {
        when(estudianteRepository.findAll()).thenReturn(List.of(estudianteMock));

        InstanciaComun instancia = new InstanciaComun();
        Incidencia incidencia = new Incidencia();

        when(instanciaComunRepository.findByEstudianteYPeriodo(
                any(), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of(instancia, instancia));

        when(incidenciaRepository.findByEstudianteYPeriodo(
                any(), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of(incidencia));

        when(recordatorioRepository.findByEstudianteYPeriodo(
                any(), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of());

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, null, null, null, null);

        assertEquals(2, resultado.getTotalInstancias());
        assertEquals(1, resultado.getTotalIncidencias());
        assertEquals(0, resultado.getTotalRecordatorios());
    }

    @Test
    @DisplayName("Generar reporte - Conteo de recordatorios por estado operativo correcto")
    void generarReporte_ConteoRecordatoriosPorEstadoCorrecto() {
        when(estudianteRepository.findAll()).thenReturn(List.of(estudianteMock));

        lenient().when(instanciaComunRepository.findByEstudianteYPeriodo(
                        any(), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of());
        lenient().when(incidenciaRepository.findByEstudianteYPeriodo(
                        any(), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of());

        List<Recordatorio> recordatorios = List.of(
                buildRecordatorio(EstadoRecordatorio.VENCIDO),
                buildRecordatorio(EstadoRecordatorio.VENCIDO),
                buildRecordatorio(EstadoRecordatorio.HOY),
                buildRecordatorio(EstadoRecordatorio.FUTURO)
        );

        when(recordatorioRepository.findByEstudianteYPeriodo(
                any(), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(recordatorios);

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, null, null, null, null);

        assertEquals(4, resultado.getTotalRecordatorios());
        assertEquals(2, resultado.getRecordatoriosVencidos());
        assertEquals(1, resultado.getRecordatoriosHoy());
        assertEquals(1, resultado.getRecordatoriosFuturos());
    }

    // =========================================================
    // VALIDACIÓN DE FECHAS
    // =========================================================

    @Test
    @DisplayName("Generar reporte - Fecha desde posterior a hasta → 400")
    void generarReporte_FechasInvertidas_Lanza400() {
        LocalDate desde = LocalDate.now();
        LocalDate hasta = LocalDate.now().minusDays(1);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reporteService.generarReporte(null, null, null, desde, hasta));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(estudianteRepository, never()).findAll();
    }

    // =========================================================
    // PERÍODO Y FILTROS APLICADOS
    // =========================================================

    @Test
    @DisplayName("Generar reporte - Período por defecto es últimos 3 meses")
    void generarReporte_SinPeriodo_UsaUltimos3Meses() {
        when(estudianteRepository.findAll()).thenReturn(List.of());

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, null, null, null, null);

        assertEquals(LocalDate.now().minusMonths(3), resultado.getDesde());
        assertEquals(LocalDate.now(), resultado.getHasta());
    }

    @Test
    @DisplayName("Generar reporte - Filtros aplicados se reflejan en la descripción")
    void generarReporte_FiltrosAplicados_SeReflejanEnDescripcion() {
        when(estudianteRepository.findById(5)).thenReturn(Optional.of(estudianteMock));
        mockearMetricasVacias(estudianteMock.getIdUsuario());

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, null, 5,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 6, 30));

        assertNotNull(resultado.getFiltrosAplicados());
        assertTrue(resultado.getFiltrosAplicados().contains("Estudiante ID: 5"));
        assertTrue(resultado.getFiltrosAplicados().contains("2025-01-01"));
    }
}