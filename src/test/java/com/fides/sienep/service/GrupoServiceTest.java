package com.fides.sienep.service;

import com.fides.sienep.dto.GrupoRequestDTO;
import com.fides.sienep.dto.GrupoResponseDTO;
import com.fides.sienep.dto.GrupoUpdateDTO;
import com.fides.sienep.mapper.GrupoMapper;
import com.fides.sienep.model.Carrera;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Grupo;
import com.fides.sienep.repository.CarreraRepository;
import com.fides.sienep.repository.GrupoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrupoServiceTest {

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private CarreraRepository carreraRepository;

    @Mock
    private GrupoMapper grupoMapper;

    @InjectMocks
    private GrupoService grupoService;

    private Carrera carreraMock;
    private Grupo grupoMock;
    private GrupoRequestDTO requestDTO;
    private GrupoResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        carreraMock = new Carrera();
        carreraMock.setIdCarrera(1);
        carreraMock.setNomCarrera("Licenciatura en TI");
        carreraMock.setEstado(EstadoLogico.ACTIVO);

        grupoMock = Grupo.builder()
                .idGrupo(1)
                .nomGrupo("Grupo A")
                .generacion(LocalDate.now().getYear())
                .carrera(carreraMock)
                .estado(EstadoLogico.ACTIVO)
                .build();

        requestDTO = new GrupoRequestDTO();
        requestDTO.setNomGrupo("Grupo A");
        requestDTO.setIdCarrera(1);
        requestDTO.setGeneracion(LocalDate.now().getYear());

        responseDTO = GrupoResponseDTO.builder()
                .idGrupo(1)
                .nomGrupo("Grupo A")
                .generacion(LocalDate.now().getYear())
                .estado(EstadoLogico.ACTIVO)
                .idCarrera(1)
                .nomCarrera("Licenciatura en TI")
                .build();
    }

    // =========================================================
    // CREAR
    // =========================================================

    @Test
    @DisplayName("Crear grupo - Éxito")
    void crear_Exito() {
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carreraMock));
        when(grupoRepository.existsByNomGrupoAndCarreraIdCarreraAndGeneracionAndEstado(
                any(), any(), any(), any())).thenReturn(false);
        when(grupoRepository.save(any(Grupo.class))).thenReturn(grupoMock);
        when(grupoMapper.toDto(any(Grupo.class))).thenReturn(responseDTO);

        GrupoResponseDTO result = grupoService.crear(requestDTO);

        assertNotNull(result);
        assertEquals("Grupo A", result.getNomGrupo());
        assertEquals(1, result.getIdCarrera());
        verify(grupoRepository).save(any(Grupo.class));
    }

    @Test
    @DisplayName("Crear grupo - Carrera no encontrada → 404")
    void crear_LanzaExcepcion_CarreraNoEncontrada() {
        when(carreraRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> grupoService.crear(requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(grupoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear grupo - Carrera inactiva → 409")
    void crear_LanzaExcepcion_CarreraInactiva() {
        carreraMock.setEstado(EstadoLogico.INACTIVO);
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carreraMock));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> grupoService.crear(requestDTO));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(grupoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear grupo - Generación futura → 400")
    void crear_LanzaExcepcion_GeneracionFutura() {
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carreraMock));
        requestDTO.setGeneracion(LocalDate.now().getYear() + 1);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> grupoService.crear(requestDTO));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(grupoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear grupo - Nombre duplicado en misma carrera y generación → 409")
    void crear_LanzaExcepcion_NombreDuplicado() {
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carreraMock));
        when(grupoRepository.existsByNomGrupoAndCarreraIdCarreraAndGeneracionAndEstado(
                any(), any(), any(), any())).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> grupoService.crear(requestDTO));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(grupoRepository, never()).save(any());
    }

    // =========================================================
    // OBTENER POR ID
    // =========================================================

    @Test
    @DisplayName("Obtener por ID - Éxito")
    void obtenerPorId_Exito() {
        when(grupoRepository.findById(1)).thenReturn(Optional.of(grupoMock));
        when(grupoMapper.toDto(grupoMock)).thenReturn(responseDTO);

        GrupoResponseDTO result = grupoService.obtenerPorId(1);

        assertNotNull(result);
        assertEquals(1, result.getIdGrupo());
        verify(grupoMapper).toDto(grupoMock);
    }

    @Test
    @DisplayName("Obtener por ID - No encontrado → 404")
    void obtenerPorId_LanzaExcepcion_NoEncontrado() {
        when(grupoRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> grupoService.obtenerPorId(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(grupoMapper, never()).toDto(any());
    }

    // =========================================================
    // LISTAR
    // =========================================================

    @Test
    @DisplayName("Listar grupos activos - sin filtros")
    void listar_SinFiltros_DevuelveActivos() {
        List<Grupo> grupos = List.of(grupoMock);
        List<GrupoResponseDTO> dtos = List.of(responseDTO);

        when(grupoRepository.findByEstado(EstadoLogico.ACTIVO)).thenReturn(grupos);
        when(grupoMapper.toDtoList(grupos)).thenReturn(dtos);

        List<GrupoResponseDTO> result = grupoService.listar(null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Listar grupos - filtrado por carrera")
    void listar_FiltradoPorCarrera() {
        List<Grupo> grupos = List.of(grupoMock);
        List<GrupoResponseDTO> dtos = List.of(responseDTO);

        when(grupoRepository.findByCarreraIdCarreraAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(grupos);
        when(grupoMapper.toDtoList(grupos)).thenReturn(dtos);

        List<GrupoResponseDTO> result = grupoService.listar(1, null);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getIdCarrera());
    }

    // =========================================================
    // ACTUALIZAR
    // =========================================================

    @Test
    @DisplayName("Actualizar grupo - Éxito")
    void actualizar_Exito() {
        GrupoUpdateDTO updateDTO = new GrupoUpdateDTO();
        updateDTO.setNomGrupo("Grupo B");
        updateDTO.setGeneracion(LocalDate.now().getYear());

        when(grupoRepository.findById(1)).thenReturn(Optional.of(grupoMock));
        when(grupoRepository.findByNomGrupoAndCarreraIdCarreraAndGeneracionAndEstado(
                any(), any(), any(), any())).thenReturn(Optional.empty());
        when(grupoRepository.save(any(Grupo.class))).thenReturn(grupoMock);
        when(grupoMapper.toDto(any(Grupo.class))).thenReturn(responseDTO);

        GrupoResponseDTO result = grupoService.actualizar(1, updateDTO);

        assertNotNull(result);
        assertEquals("Grupo B", grupoMock.getNomGrupo());
        verify(grupoRepository).save(grupoMock);
    }

    @Test
    @DisplayName("Actualizar grupo - Rechaza editar un grupo inactivo (M3)")
    void actualizar_LanzaConflicto_GrupoInactivo() {
        grupoMock.setEstado(EstadoLogico.INACTIVO);
        GrupoUpdateDTO updateDTO = new GrupoUpdateDTO();
        updateDTO.setNomGrupo("Grupo B");
        updateDTO.setGeneracion(LocalDate.now().getYear());

        when(grupoRepository.findById(1)).thenReturn(Optional.of(grupoMock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> grupoService.actualizar(1, updateDTO));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(grupoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar grupo - No encontrado → 404")
    void actualizar_LanzaExcepcion_NoEncontrado() {
        GrupoUpdateDTO updateDTO = new GrupoUpdateDTO();
        updateDTO.setNomGrupo("Grupo B");
        updateDTO.setGeneracion(LocalDate.now().getYear());

        when(grupoRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> grupoService.actualizar(99, updateDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(grupoRepository, never()).save(any());
    }

    // =========================================================
    // BAJA LÓGICA
    // =========================================================

    @Test
    @DisplayName("Dar de baja - Estado pasa a INACTIVO")
    void darDeBaja_DebePonerEstadoInactivo() {
        when(grupoRepository.findById(1)).thenReturn(Optional.of(grupoMock));
        when(grupoRepository.save(any(Grupo.class))).thenReturn(grupoMock);

        grupoService.darDeBaja(1);

        assertEquals(EstadoLogico.INACTIVO, grupoMock.getEstado());
        verify(grupoRepository).save(grupoMock);
        verify(grupoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Dar de baja - No encontrado → 404")
    void darDeBaja_LanzaExcepcion_NoEncontrado() {
        when(grupoRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> grupoService.darDeBaja(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(grupoRepository, never()).save(any());
        verify(grupoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Dar de baja - Ya está inactivo (409) (H11)")
    void darDeBaja_LanzaConflict_YaInactivo() {
        grupoMock.setEstado(EstadoLogico.INACTIVO);
        when(grupoRepository.findById(1)).thenReturn(Optional.of(grupoMock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> grupoService.darDeBaja(1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(grupoRepository, never()).save(any());
    }

    // =========================================================
    // REACTIVAR (M4)
    // =========================================================

    @Test
    @DisplayName("Reactivar - Estado pasa a ACTIVO")
    void reactivar_DebePonerEstadoActivo() {
        grupoMock.setEstado(EstadoLogico.INACTIVO);
        when(grupoRepository.findById(1)).thenReturn(Optional.of(grupoMock));
        when(grupoRepository.save(any(Grupo.class))).thenReturn(grupoMock);

        grupoService.reactivar(1);

        assertEquals(EstadoLogico.ACTIVO, grupoMock.getEstado());
        verify(grupoRepository).save(grupoMock);
    }

    @Test
    @DisplayName("Reactivar - No encontrado → 404")
    void reactivar_LanzaExcepcion_NoEncontrado() {
        when(grupoRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> grupoService.reactivar(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(grupoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactivar - Ya está activo (409) (H11)")
    void reactivar_LanzaConflict_YaActivo() {
        when(grupoRepository.findById(1)).thenReturn(Optional.of(grupoMock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> grupoService.reactivar(1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(grupoRepository, never()).save(any());
    }
}