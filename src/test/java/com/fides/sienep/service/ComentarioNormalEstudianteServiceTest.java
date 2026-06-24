package com.fides.sienep.service;

import com.fides.sienep.dto.ComentarioNormalEstudianteRequestDTO;
import com.fides.sienep.dto.ComentarioNormalEstudianteResponseDTO;
import com.fides.sienep.dto.ComentarioNormalEstudianteUpdateDTO;
import com.fides.sienep.mapper.ComentarioNormalEstudianteMapper;
import com.fides.sienep.model.ComentarioNormalEstudiante;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.model.Rol;
import com.fides.sienep.repository.ComentarioNormalEstudianteRepository;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComentarioNormalEstudianteServiceTest {

    @Mock
    private ComentarioNormalEstudianteRepository comentarioRepository;

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private ComentarioNormalEstudianteMapper comentarioMapper;

    @InjectMocks
    private ComentarioNormalEstudianteService comentarioService;

    private Estudiante estudianteMock;
    private Funcionario autorMock;
    private ComentarioNormalEstudiante comentarioMock;
    private ComentarioNormalEstudianteResponseDTO responseDTO;

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

        comentarioMock = new ComentarioNormalEstudiante(
                "El estudiante muestra avances.", autorMock, estudianteMock
        );

        responseDTO = ComentarioNormalEstudianteResponseDTO.builder()
                .id(1)
                .contenido("El estudiante muestra avances.")
                .estado(EstadoLogico.ACTIVO)
                .idEstudiante(1)
                .idAutor(1)
                .nombreAutor("Ana López")
                .build();
    }

    // =========================================================
    // CREAR
    // =========================================================

    @Test
    @DisplayName("Crear comentario normal - Éxito")
    void crear_Exito() {
        ComentarioNormalEstudianteRequestDTO dto = new ComentarioNormalEstudianteRequestDTO();
        dto.setContenido("El estudiante muestra avances.");
        dto.setIdEstudiante(1);
        dto.setIdAutor(1);

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(autorMock));
        when(comentarioRepository.save(any(ComentarioNormalEstudiante.class))).thenReturn(comentarioMock);
        when(comentarioMapper.toDto(any(ComentarioNormalEstudiante.class))).thenReturn(responseDTO);

        ComentarioNormalEstudianteResponseDTO result = comentarioService.crear(dto);

        assertNotNull(result);
        assertEquals("El estudiante muestra avances.", result.getContenido());
        verify(comentarioRepository).save(any(ComentarioNormalEstudiante.class));
    }

    @Test
    @DisplayName("Crear comentario - Estudiante no encontrado → 404")
    void crear_LanzaExcepcion_EstudianteNoEncontrado() {
        ComentarioNormalEstudianteRequestDTO dto = new ComentarioNormalEstudianteRequestDTO();
        dto.setContenido("Contenido");
        dto.setIdEstudiante(99);
        dto.setIdAutor(1);

        when(estudianteRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> comentarioService.crear(dto));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear comentario - Autor no encontrado → 404")
    void crear_LanzaExcepcion_AutorNoEncontrado() {
        ComentarioNormalEstudianteRequestDTO dto = new ComentarioNormalEstudianteRequestDTO();
        dto.setContenido("Contenido");
        dto.setIdEstudiante(1);
        dto.setIdAutor(99);

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
    @DisplayName("Editar comentario - Éxito")
    void editar_Exito() {
        ComentarioNormalEstudianteUpdateDTO updateDTO = new ComentarioNormalEstudianteUpdateDTO();
        updateDTO.setContenido("Contenido actualizado.");

        when(comentarioRepository.findById(1)).thenReturn(Optional.of(comentarioMock));
        when(comentarioRepository.save(any(ComentarioNormalEstudiante.class))).thenReturn(comentarioMock);
        when(comentarioMapper.toDto(any(ComentarioNormalEstudiante.class))).thenReturn(responseDTO);

        ComentarioNormalEstudianteResponseDTO result = comentarioService.editar(1, updateDTO);

        assertNotNull(result);
        assertEquals("Contenido actualizado.", comentarioMock.getContenido());
        verify(comentarioRepository).save(comentarioMock);
    }

    @Test
    @DisplayName("Editar comentario - No encontrado → 404")
    void editar_LanzaExcepcion_NoEncontrado() {
        ComentarioNormalEstudianteUpdateDTO updateDTO = new ComentarioNormalEstudianteUpdateDTO();
        updateDTO.setContenido("Contenido actualizado.");

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
    @DisplayName("Eliminar comentario - Estado pasa a ELIMINADO")
    void eliminar_DebePonerEstadoEliminado() {
        when(comentarioRepository.findById(1)).thenReturn(Optional.of(comentarioMock));
        when(comentarioRepository.save(any(ComentarioNormalEstudiante.class))).thenReturn(comentarioMock);

        comentarioService.eliminar(1);

        assertEquals(EstadoLogico.ELIMINADO, comentarioMock.getEstado());
        verify(comentarioRepository).save(comentarioMock);
        verify(comentarioRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Eliminar comentario - No encontrado → 404")
    void eliminar_LanzaExcepcion_NoEncontrado() {
        when(comentarioRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> comentarioService.eliminar(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Listar por estudiante - Éxito (funcionario consulta cualquier estudiante)")
    void listarPorEstudiante_Exito() {
        List<ComentarioNormalEstudiante> comentarios = List.of(comentarioMock);

        when(comentarioRepository.findByEstudianteIdUsuarioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(comentarios);
        when(comentarioMapper.toDto(comentarioMock)).thenReturn(responseDTO);

        // Simulamos un usuario autenticado que NO es estudiante
        org.springframework.security.core.Authentication auth =
                mock(org.springframework.security.core.Authentication.class);
        lenient().when(auth.getAuthorities()).thenReturn(List.of());
        org.springframework.security.core.context.SecurityContextHolder
                .getContext().setAuthentication(auth);

        List<ComentarioNormalEstudianteResponseDTO> result =
                comentarioService.listarPorEstudiante(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(comentarioRepository).findByEstudianteIdUsuarioAndEstado(1, EstadoLogico.ACTIVO);
    }

    @Test
    @DisplayName("Listar por estudiante - Falta idEstudiante → 400")
    void listarPorEstudiante_LanzaExcepcion_SinIdEstudiante() {

        // Simulamos un usuario autenticado que NO es estudiante
        // y no pasa idEstudiante → el service lanza 400
        org.springframework.security.core.Authentication auth =
                mock(org.springframework.security.core.Authentication.class);
        lenient().when(auth.getAuthorities()).thenReturn(List.of());
        org.springframework.security.core.context.SecurityContextHolder
                .getContext().setAuthentication(auth);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> comentarioService.listarPorEstudiante(null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(comentarioRepository, never()).findByEstudianteIdUsuarioAndEstado(any(), any());
    }
}