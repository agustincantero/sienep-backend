package com.fides.sienep.service;

import com.fides.sienep.dto.ComentarioConfidencialEstudianteRequestDTO;
import com.fides.sienep.dto.ComentarioConfidencialEstudianteResponseDTO;
import com.fides.sienep.dto.ComentarioConfidencialEstudianteUpdateDTO;
import com.fides.sienep.mapper.ComentarioConfidencialEstudianteMapper;
import com.fides.sienep.model.ComentarioConfidencialEstudiante;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.model.Rol;
import com.fides.sienep.repository.ComentarioConfidencialEstudianteRepository;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComentarioConfidencialEstudianteServiceTest {

    @Mock
    private ComentarioConfidencialEstudianteRepository comentarioRepository;

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private ComentarioConfidencialEstudianteMapper comentarioMapper;

    @InjectMocks
    private ComentarioConfidencialEstudianteService comentarioService;

    private Estudiante estudianteMock;
    private Funcionario autorMock;
    private ComentarioConfidencialEstudiante comentarioMock;
    private ComentarioConfidencialEstudianteResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        Rol rol = new Rol("Psicopedagogo");

        autorMock = new Funcionario(
                "Ana", "López", "12345678",
                PaisDocumento.ARG, "hash",
                "ana.lopez@utec.edu.uy", rol
        );

        estudianteMock = new Estudiante();
        estudianteMock.setNombre("Pedro");
        estudianteMock.setApellido("Díaz");
        estudianteMock.setDocumento("12345679");
        estudianteMock.setPaisDocumento(PaisDocumento.ARG);
        estudianteMock.setEmail("pedro.diaz@utec.edu.uy");
        estudianteMock.setFechaNacimiento(LocalDate.of(2000, 1, 1));
        estudianteMock.setEstado(EstadoLogico.ACTIVO);
        estudianteMock.setTelefonos(new ArrayList<>());
        estudianteMock.setGrupos(new ArrayList<>());

        comentarioMock = new ComentarioConfidencialEstudiante(
                "Observación confidencial.", autorMock, estudianteMock
        );

        responseDTO = ComentarioConfidencialEstudianteResponseDTO.builder()
                .id(1)
                .contenido("Observación confidencial.")
                .estado(EstadoLogico.ACTIVO)
                .idEstudiante(1)
                .idAutor(1)
                .nombreAutor("Ana López")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockAutenticadoComoFuncionario(int id) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(String.valueOf(id));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // =========================================================
    // CREAR
    // =========================================================

    @Test
    @DisplayName("Crear observación confidencial - Éxito")
    void crear_Exito() {
        mockAutenticadoComoFuncionario(1);

        ComentarioConfidencialEstudianteRequestDTO dto = new ComentarioConfidencialEstudianteRequestDTO();
        dto.setContenido("Observación confidencial.");
        dto.setIdEstudiante(1);

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(autorMock));
        when(comentarioRepository.save(any(ComentarioConfidencialEstudiante.class))).thenReturn(comentarioMock);
        when(comentarioMapper.toDto(any(ComentarioConfidencialEstudiante.class))).thenReturn(responseDTO);

        ComentarioConfidencialEstudianteResponseDTO result = comentarioService.crear(dto);

        assertNotNull(result);
        assertEquals("Observación confidencial.", result.getContenido());
        verify(comentarioRepository).save(any(ComentarioConfidencialEstudiante.class));
    }

    @Test
    @DisplayName("Crear observación - Estudiante no encontrado → 404")
    void crear_LanzaExcepcion_EstudianteNoEncontrado() {
        ComentarioConfidencialEstudianteRequestDTO dto = new ComentarioConfidencialEstudianteRequestDTO();
        dto.setContenido("Contenido");
        dto.setIdEstudiante(99);

        when(estudianteRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> comentarioService.crear(dto));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear observación - Autor no encontrado → 404")
    void crear_LanzaExcepcion_AutorNoEncontrado() {
        mockAutenticadoComoFuncionario(99);

        ComentarioConfidencialEstudianteRequestDTO dto = new ComentarioConfidencialEstudianteRequestDTO();
        dto.setContenido("Contenido");
        dto.setIdEstudiante(1);

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));
        when(funcionarioRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> comentarioService.crear(dto));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(comentarioRepository, never()).save(any());
    }

    // =========================================================
    // OBTENER POR ID
    // =========================================================

    @Test
    @DisplayName("Obtener por ID - No encontrado → 404")
    void obtenerPorId_LanzaExcepcion_NoEncontrado() {
        when(comentarioRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> comentarioService.obtenerPorId(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(comentarioMapper, never()).toDto(any());
    }

    // =========================================================
    // EDITAR
    // =========================================================

    @Test
    @DisplayName("Editar observación - Éxito")
    void editar_Exito() {
        ComentarioConfidencialEstudianteUpdateDTO updateDTO = new ComentarioConfidencialEstudianteUpdateDTO();
        updateDTO.setContenido("Observación actualizada.");

        when(comentarioRepository.findById(1)).thenReturn(Optional.of(comentarioMock));
        when(comentarioRepository.save(any(ComentarioConfidencialEstudiante.class))).thenReturn(comentarioMock);
        when(comentarioMapper.toDto(any(ComentarioConfidencialEstudiante.class))).thenReturn(responseDTO);

        ComentarioConfidencialEstudianteResponseDTO result = comentarioService.editar(1, updateDTO);

        assertNotNull(result);
        assertEquals("Observación actualizada.", comentarioMock.getContenido());
        verify(comentarioRepository).save(comentarioMock);
    }

    @Test
    @DisplayName("Editar observación - No encontrada → 404")
    void editar_LanzaExcepcion_NoEncontrado() {
        ComentarioConfidencialEstudianteUpdateDTO updateDTO = new ComentarioConfidencialEstudianteUpdateDTO();
        updateDTO.setContenido("Observación actualizada.");

        when(comentarioRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> comentarioService.editar(99, updateDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(comentarioRepository, never()).save(any());
    }

    // =========================================================
    // ELIMINAR (BAJA LÓGICA)
    // =========================================================

    @Test
    @DisplayName("Eliminar observación - Estado pasa a ELIMINADO")
    void eliminar_DebePonerEstadoEliminado() {
        when(comentarioRepository.findById(1)).thenReturn(Optional.of(comentarioMock));
        when(comentarioRepository.save(any(ComentarioConfidencialEstudiante.class))).thenReturn(comentarioMock);

        comentarioService.eliminar(1);

        assertEquals(EstadoLogico.ELIMINADO, comentarioMock.getEstado());
        verify(comentarioRepository).save(comentarioMock);
        verify(comentarioRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Eliminar observación - No encontrada → 404")
    void eliminar_LanzaExcepcion_NoEncontrado() {
        when(comentarioRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> comentarioService.eliminar(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(comentarioRepository, never()).save(any());
    }


    @Test
    @DisplayName("Listar por estudiante - Éxito")
    void listarPorEstudiante_Exito() {
        List<ComentarioConfidencialEstudiante> comentarios = List.of(comentarioMock);
        List<ComentarioConfidencialEstudianteResponseDTO> dtos = List.of(responseDTO);

        when(comentarioRepository.findByEstudianteIdUsuarioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(comentarios);
        when(comentarioMapper.toDto(comentarioMock)).thenReturn(responseDTO);

        List<ComentarioConfidencialEstudianteResponseDTO> result =
                comentarioService.listarPorEstudiante(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(comentarioRepository).findByEstudianteIdUsuarioAndEstado(1, EstadoLogico.ACTIVO);
    }
}