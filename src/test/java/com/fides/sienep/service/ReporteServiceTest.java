package com.fides.sienep.service;

import com.fides.sienep.dto.ReporteResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.EstadoRecordatorio;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Incidencia;
import com.fides.sienep.model.InstanciaComun;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.model.Recordatorio;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

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

        Funcionario funcionarioAutenticadoMock = new Funcionario();
        funcionarioAutenticadoMock.setNombre("Mariana");
        funcionarioAutenticadoMock.setApellido("Fernández");
        funcionarioAutenticadoMock.setDocumento("11222333");
        lenient().when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioAutenticadoMock));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Helper: mockear queries de métricas vacías ────────────
    private void mockearMetricasVacias(Integer idEstudiante) {
        List<Integer> ids = List.of(idEstudiante);
        lenient().when(instanciaComunRepository.findByEstudiantesYPeriodo(
                        eq(ids), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of());
        lenient().when(incidenciaRepository.findByEstudiantesYPeriodo(
                        eq(ids), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of());
        lenient().when(recordatorioRepository.findByEstudiantesYPeriodo(
                        eq(ids), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of());
    }

    // ── Helper: crear recordatorio mock con estado operativo ──
    // La instancia (con estudianteMock) es necesaria porque ReporteService
    // ahora agrupa los recordatorios batch por recordatorio.getInstancia()
    // .getEstudiante().getIdUsuario() (M11).
    private Recordatorio buildRecordatorio(EstadoRecordatorio estadoOperativo) {
        InstanciaComun instancia = new InstanciaComun();
        instancia.setEstudiante(estudianteMock);

        Recordatorio r = new Recordatorio();
        r.setInstancia(instancia);
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
        when(estudianteRepository.findByEstado(EstadoLogico.ACTIVO)).thenReturn(List.of(estudianteMock));
        mockearMetricasVacias(estudianteMock.getIdUsuario());

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, null, null, null, null, null);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalEstudiantes());
        assertEquals(0, resultado.getTotalInstancias());
        assertEquals(0, resultado.getTotalIncidencias());
        assertEquals(0, resultado.getTotalRecordatorios());
        assertNull(resultado.getAdvertencia());
        assertNotNull(resultado.getFechaGeneracion());
        assertEquals("11222333 - Mariana Fernández", resultado.getGeneradoPor());
    }

    @Test
    @DisplayName("Generar reporte - Funcionario autenticado no encontrado devuelve 404")
    void generarReporte_FuncionarioAutenticadoNoEncontrado_Lanza404() {
        when(estudianteRepository.findByEstado(EstadoLogico.ACTIVO)).thenReturn(List.of());
        when(funcionarioRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reporteService.generarReporte(null, null, null, null, null, null));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Generar reporte - Sin filtros solo considera estudiantes ACTIVOS (M10)")
    void generarReporte_SinFiltros_SoloConsideraActivos() {
        when(estudianteRepository.findByEstado(EstadoLogico.ACTIVO)).thenReturn(List.of(estudianteMock));
        mockearMetricasVacias(estudianteMock.getIdUsuario());

        reporteService.generarReporte(null, null, null, null, null, null);

        verify(estudianteRepository).findByEstado(EstadoLogico.ACTIVO);
        verify(estudianteRepository, never()).findAll();
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
                null, null, 1, null, null, null);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalEstudiantes());
        assertNull(resultado.getAdvertencia());
    }

    @Test
    @DisplayName("Generar reporte - Estudiante no encontrado devuelve lista vacía")
    void generarReporte_EstudianteNoEncontrado_DevuelveListaVacia() {
        when(estudianteRepository.findById(99)).thenReturn(Optional.empty());

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, null, 99, null, null, null);

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
                null, 1, null, null, null, null);

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
                1, null, null, null, null, null);

        assertNotNull(resultado.getAdvertencia());
        assertTrue(resultado.getAdvertencia().contains("múltiples grupos"));
    }

    // =========================================================
    // MÉTRICAS
    // =========================================================

    @Test
    @DisplayName("Generar reporte - Conteo de instancias e incidencias correcto")
    void generarReporte_ConteoDeMetricasCorrecto() {
        when(estudianteRepository.findByEstado(EstadoLogico.ACTIVO)).thenReturn(List.of(estudianteMock));

        InstanciaComun instancia = new InstanciaComun();
        instancia.setEstudiante(estudianteMock);
        Incidencia incidencia = new Incidencia();
        incidencia.setEstudiante(estudianteMock);

        when(instanciaComunRepository.findByEstudiantesYPeriodo(
                any(), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of(instancia, instancia));

        when(incidenciaRepository.findByEstudiantesYPeriodo(
                any(), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of(incidencia));

        when(recordatorioRepository.findByEstudiantesYPeriodo(
                any(), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of());

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, null, null, null, null, null);

        assertEquals(2, resultado.getTotalInstancias());
        assertEquals(1, resultado.getTotalIncidencias());
        assertEquals(0, resultado.getTotalRecordatorios());
    }

    @Test
    @DisplayName("Generar reporte - Con varios estudiantes hace una sola query por tipo y agrupa bien las métricas (M11)")
    void generarReporte_VariosEstudiantes_UnaSolaQueryPorTipoYAgrupaBien() {
        estudianteMock.setIdUsuario(1);

        Estudiante estudianteMock2 = new Estudiante();
        estudianteMock2.setIdUsuario(2);
        estudianteMock2.setNombre("Luis");
        estudianteMock2.setApellido("Pérez");
        estudianteMock2.setDocumento("87654321");
        estudianteMock2.setPaisDocumento(PaisDocumento.ARG);
        estudianteMock2.setEmail("luis.perez@utec.edu.uy");
        estudianteMock2.setFechaNacimiento(LocalDate.of(1999, 5, 20));
        estudianteMock2.setEstado(EstadoLogico.ACTIVO);
        estudianteMock2.setTelefonos(new ArrayList<>());
        estudianteMock2.setGrupos(new ArrayList<>());

        when(estudianteRepository.findByEstado(EstadoLogico.ACTIVO))
                .thenReturn(List.of(estudianteMock, estudianteMock2));

        InstanciaComun instanciaDeUno = new InstanciaComun();
        instanciaDeUno.setEstudiante(estudianteMock);
        InstanciaComun instanciaDeDos = new InstanciaComun();
        instanciaDeDos.setEstudiante(estudianteMock2);

        when(instanciaComunRepository.findByEstudiantesYPeriodo(
                eq(List.of(1, 2)), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of(instanciaDeUno, instanciaDeDos, instanciaDeDos));
        when(incidenciaRepository.findByEstudiantesYPeriodo(
                eq(List.of(1, 2)), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of());
        when(recordatorioRepository.findByEstudiantesYPeriodo(
                eq(List.of(1, 2)), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of());

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, null, null, null, null, null);

        // Una sola llamada por tipo, sin importar cuántos estudiantes haya (no N+1)
        verify(instanciaComunRepository, times(1))
                .findByEstudiantesYPeriodo(any(), any(), any(), any());
        verify(incidenciaRepository, times(1))
                .findByEstudiantesYPeriodo(any(), any(), any(), any());
        verify(recordatorioRepository, times(1))
                .findByEstudiantesYPeriodo(any(), any(), any(), any());

        assertEquals(2, resultado.getTotalEstudiantes());
        assertEquals(3, resultado.getTotalInstancias());

        // Las métricas quedan asignadas al estudiante correcto, no mezcladas
        assertEquals(1, resultado.getDetallePorEstudiante().get(0).getCantidadInstancias());
        assertEquals(2, resultado.getDetallePorEstudiante().get(1).getCantidadInstancias());
    }

    @Test
    @DisplayName("Generar reporte - Conteo de recordatorios por estado operativo correcto")
    void generarReporte_ConteoRecordatoriosPorEstadoCorrecto() {
        when(estudianteRepository.findByEstado(EstadoLogico.ACTIVO)).thenReturn(List.of(estudianteMock));

        lenient().when(instanciaComunRepository.findByEstudiantesYPeriodo(
                        any(), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of());
        lenient().when(incidenciaRepository.findByEstudiantesYPeriodo(
                        any(), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(List.of());

        List<Recordatorio> recordatorios = List.of(
                buildRecordatorio(EstadoRecordatorio.VENCIDO),
                buildRecordatorio(EstadoRecordatorio.VENCIDO),
                buildRecordatorio(EstadoRecordatorio.HOY),
                buildRecordatorio(EstadoRecordatorio.FUTURO)
        );

        when(recordatorioRepository.findByEstudiantesYPeriodo(
                any(), eq(EstadoLogico.ACTIVO), any(), any()))
                .thenReturn(recordatorios);

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, null, null, null, null, null);

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
                () -> reporteService.generarReporte(null, null, null, desde, hasta, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(estudianteRepository, never()).findByEstado(EstadoLogico.ACTIVO);
    }

    // =========================================================
    // PERÍODO Y FILTROS APLICADOS
    // =========================================================

    @Test
    @DisplayName("Generar reporte - Período por defecto es últimos 3 meses")
    void generarReporte_SinPeriodo_UsaUltimos3Meses() {
        when(estudianteRepository.findByEstado(EstadoLogico.ACTIVO)).thenReturn(List.of());

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, null, null, null, null, null);

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
                LocalDate.of(2025, 6, 30), null);

        assertNotNull(resultado.getFiltrosAplicados());
        assertTrue(resultado.getFiltrosAplicados().contains("Estudiante ID: 5"));
        assertTrue(resultado.getFiltrosAplicados().contains("2025-01-01"));
    }

    // =========================================================
    // PAGINACIÓN (H1)
    // =========================================================

    @Test
    @DisplayName("Generar reporte - Con Pageable usa la query paginada y completa la metadata")
    void generarReporte_ConPageable_UsaQueryPaginadaYCompletaMetadata() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Estudiante> pagina = new PageImpl<>(List.of(estudianteMock), pageable, 1);
        when(estudianteRepository.findByEstado(EstadoLogico.ACTIVO, pageable)).thenReturn(pagina);
        mockearMetricasVacias(estudianteMock.getIdUsuario());

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, null, null, null, null, pageable);

        verify(estudianteRepository).findByEstado(EstadoLogico.ACTIVO, pageable);
        verify(estudianteRepository, never()).findByEstado(EstadoLogico.ACTIVO);
        assertEquals(0, resultado.getPagina());
        assertEquals(20, resultado.getTamanioPagina());
        assertEquals(1, resultado.getTotalPaginas());
        assertEquals(1L, resultado.getTotalEstudiantesFiltrados());
    }

    @Test
    @DisplayName("Generar reporte - Sin Pageable (exportación PDF) no completa metadata de paginación")
    void generarReporte_SinPageable_NoCompletaMetadataDePaginacion() {
        when(estudianteRepository.findByEstado(EstadoLogico.ACTIVO)).thenReturn(List.of(estudianteMock));
        mockearMetricasVacias(estudianteMock.getIdUsuario());

        ReporteResponseDTO resultado = reporteService.generarReporte(
                null, null, null, null, null, null);

        assertNull(resultado.getPagina());
        assertNull(resultado.getTamanioPagina());
        assertNull(resultado.getTotalPaginas());
        assertNull(resultado.getTotalEstudiantesFiltrados());
    }

    @Test
    @DisplayName("Generar reporte - Con Pageable y filtro de grupo usa findByGrupoAndEstado paginado")
    void generarReporte_ConPageableYGrupo_UsaQueryPaginadaDeGrupo() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Estudiante> pagina = new PageImpl<>(List.of(estudianteMock), pageable, 1);
        when(estudianteRepository.findByGrupoAndEstado(1, EstadoLogico.ACTIVO, pageable)).thenReturn(pagina);
        mockearMetricasVacias(estudianteMock.getIdUsuario());

        reporteService.generarReporte(null, 1, null, null, null, pageable);

        verify(estudianteRepository).findByGrupoAndEstado(1, EstadoLogico.ACTIVO, pageable);
        verify(estudianteRepository, never()).findByGrupoAndEstadoList(anyInt(), any());
    }

    @Test
    @DisplayName("Generar reporte - Con Pageable y filtro de carrera usa findByCarreraAndEstado paginado")
    void generarReporte_ConPageableYCarrera_UsaQueryPaginadaDeCarrera() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Estudiante> pagina = new PageImpl<>(List.of(estudianteMock), pageable, 1);
        when(estudianteRepository.findByCarreraAndEstado(1, EstadoLogico.ACTIVO, pageable)).thenReturn(pagina);
        mockearMetricasVacias(estudianteMock.getIdUsuario());

        reporteService.generarReporte(1, null, null, null, null, pageable);

        verify(estudianteRepository).findByCarreraAndEstado(1, EstadoLogico.ACTIVO, pageable);
        verify(estudianteRepository, never()).findByCarreraAndEstadoList(anyInt(), any());
    }
}