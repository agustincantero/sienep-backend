package com.fides.sienep.service;

import com.fides.sienep.dto.ComentarioNormalInstanciaRequestDTO;
import com.fides.sienep.dto.ComentarioNormalInstanciaResponseDTO;
import com.fides.sienep.mapper.ComentarioNormalInstanciaMapper;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.ComentarioNormalInstanciaRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.InstanciaRepository;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComentarioNormalInstanciaServiceTest {

    @Mock private ComentarioNormalInstanciaRepository comentarioRepository;
    @Mock private InstanciaRepository instanciaRepository;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private ComentarioNormalInstanciaMapper comentarioMapper;

    @InjectMocks
    private ComentarioNormalInstanciaService comentarioService;

    private Instancia instancia;
    private Funcionario autor;
    private ComentarioNormalInstancia comentario;
    private ComentarioNormalInstanciaRequestDTO requestDTO;
    private ComentarioNormalInstanciaResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        instancia = mock(Instancia.class);
        lenient().when(instancia.getCodInstancia()).thenReturn(1);

        autor = new Funcionario();
        autor.setIdUsuario(2);
        autor.setNombre("Juan");
        autor.setApellido("García");
        autor.setEstado(EstadoLogico.ACTIVO);

        comentario = new ComentarioNormalInstancia();
        comentario.setIdComentario(1);
        comentario.setContenido("Comentario de prueba");
        comentario.setInstancia(instancia);
        comentario.setAutor(autor);
        comentario.setEstado(EstadoLogico.ACTIVO);

        requestDTO = new ComentarioNormalInstanciaRequestDTO();
        requestDTO.setContenido("Comentario de prueba");

        responseDTO = new ComentarioNormalInstanciaResponseDTO();
        responseDTO.setIdComentario(1);
        responseDTO.setCodInstancia(1);
        responseDTO.setContenido("Comentario de prueba");
        responseDTO.setEstado(EstadoLogico.ACTIVO);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(int idUsuario) {
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(auth.getName()).thenReturn(String.valueOf(idUsuario));
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Crear comentario normal - Éxito")
    void crear_Exito() {
        mockSecurityContext(2);
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(autor));
        when(comentarioRepository.save(any(ComentarioNormalInstancia.class))).thenReturn(comentario);
        when(comentarioMapper.toDto(comentario)).thenReturn(responseDTO);

        ComentarioNormalInstanciaResponseDTO resultado = comentarioService.crear(1, requestDTO);

        assertNotNull(resultado);
        assertEquals(1, resultado.getIdComentario());
        verify(comentarioRepository).save(any(ComentarioNormalInstancia.class));
    }

    @Test
    @DisplayName("Crear comentario normal - Instancia no encontrada (404)")
    void crear_LanzaNotFound_InstanciaNoExiste() {
        mockSecurityContext(2);
        when(instanciaRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> comentarioService.crear(1, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear comentario normal - Funcionario no encontrado (404)")
    void crear_LanzaNotFound_FuncionarioNoExiste() {
        mockSecurityContext(2);
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> comentarioService.crear(1, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Listar por instancia - Éxito")
    void listarPorInstancia_Exito() {
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(comentarioRepository.findByInstancia_CodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(List.of(comentario));
        when(comentarioMapper.toDtoList(List.of(comentario))).thenReturn(List.of(responseDTO));

        List<ComentarioNormalInstanciaResponseDTO> resultado = comentarioService.listarPorInstancia(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Listar por instancia - Instancia no encontrada (404)")
    void listarPorInstancia_LanzaNotFound() {
        when(instanciaRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> comentarioService.listarPorInstancia(1));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Actualizar comentario normal - Éxito")
    void actualizar_Exito() {
        when(comentarioRepository.findByIdComentarioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(comentario));
        when(comentarioRepository.save(any(ComentarioNormalInstancia.class))).thenReturn(comentario);
        when(comentarioMapper.toDto(comentario)).thenReturn(responseDTO);

        ComentarioNormalInstanciaResponseDTO resultado = comentarioService.actualizar(1, requestDTO);

        assertNotNull(resultado);
        verify(comentarioRepository).save(comentario);
    }

    @Test
    @DisplayName("Actualizar comentario normal - No encontrado (404)")
    void actualizar_LanzaNotFound() {
        when(comentarioRepository.findByIdComentarioAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> comentarioService.actualizar(99, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dar de baja comentario normal - Éxito")
    void darDeBaja_Exito() {
        when(comentarioRepository.findByIdComentarioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(comentario));

        comentarioService.darDeBaja(1);

        assertEquals(EstadoLogico.ELIMINADO, comentario.getEstado());
        verify(comentarioRepository).save(comentario);
    }

    @Test
    @DisplayName("Dar de baja comentario normal - No encontrado (404)")
    void darDeBaja_LanzaNotFound() {
        when(comentarioRepository.findByIdComentarioAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> comentarioService.darDeBaja(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}