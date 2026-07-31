package com.fides.sienep.service;

import com.fides.sienep.dto.ComentarioConfidencialInstanciaRequestDTO;
import com.fides.sienep.dto.ComentarioConfidencialInstanciaResponseDTO;
import com.fides.sienep.mapper.ComentarioConfidencialInstanciaMapper;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.ComentarioConfidencialInstanciaRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
class ComentarioConfidencialInstanciaServiceTest {

    @Mock private ComentarioConfidencialInstanciaRepository comentarioRepository;
    @Mock private InstanciaRepository instanciaRepository;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private ComentarioConfidencialInstanciaMapper comentarioMapper;

    @InjectMocks
    private ComentarioConfidencialInstanciaService comentarioService;

    private Instancia instancia;
    private Funcionario autor;
    private ComentarioConfidencialInstancia comentario;
    private ComentarioConfidencialInstanciaRequestDTO requestDTO;
    private ComentarioConfidencialInstanciaResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        instancia = mock(Instancia.class);
        lenient().when(instancia.getCodInstancia()).thenReturn(1);

        autor = new Funcionario();
        autor.setIdUsuario(2);
        autor.setNombre("Juan");
        autor.setApellido("García");
        autor.setEstado(EstadoLogico.ACTIVO);

        ComentarioConfidencialInstancia.ComentarioConfidencialInstanciaId id =
                new ComentarioConfidencialInstancia.ComentarioConfidencialInstanciaId();
        id.setIdComConfidencial(1);
        id.setCodInstancia(1);

        comentario = new ComentarioConfidencialInstancia();
        comentario.setId(id);
        comentario.setContenido("Comentario confidencial de prueba");
        comentario.setInstancia(instancia);
        comentario.setAutor(autor);
        comentario.setEstado(EstadoLogico.ACTIVO);

        requestDTO = new ComentarioConfidencialInstanciaRequestDTO();
        requestDTO.setContenido("Comentario confidencial de prueba");

        responseDTO = new ComentarioConfidencialInstanciaResponseDTO();
        responseDTO.setIdComConfidencial(1);
        responseDTO.setCodInstancia(1);
        responseDTO.setContenido("Comentario confidencial de prueba");
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
    @DisplayName("Crear comentario confidencial - Éxito")
    void crear_Exito() {
        mockSecurityContext(2);
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(autor));
        when(comentarioRepository.save(any(ComentarioConfidencialInstancia.class))).thenReturn(comentario);
        when(comentarioMapper.toDto(comentario)).thenReturn(responseDTO);

        ComentarioConfidencialInstanciaResponseDTO resultado = comentarioService.crear(1, requestDTO);

        assertNotNull(resultado);
        assertEquals(1, resultado.getIdComConfidencial());
        verify(comentarioRepository).save(any(ComentarioConfidencialInstancia.class));
    }

    @Test
    @DisplayName("Crear comentario confidencial - Instancia no encontrada (404)")
    void crear_LanzaNotFound_InstanciaNoExiste() {
        mockSecurityContext(2);
        when(instanciaRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> comentarioService.crear(1, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear comentario confidencial - Funcionario no encontrado (404)")
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
    @DisplayName("Listar por instancia - Éxito, paginado (H18)")
    void listarPorInstancia_Exito() {
        Pageable pageable = PageRequest.of(0, 20);
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(comentarioRepository.findByInstancia_CodInstanciaAndEstado(1, EstadoLogico.ACTIVO, pageable))
                .thenReturn(new PageImpl<>(List.of(comentario), pageable, 1));
        when(comentarioMapper.toDtoList(List.of(comentario))).thenReturn(List.of(responseDTO));

        Page<ComentarioConfidencialInstanciaResponseDTO> resultado =
                comentarioService.listarPorInstancia(1, pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Listar por instancia - Instancia no encontrada (404)")
    void listarPorInstancia_LanzaNotFound() {
        Pageable pageable = PageRequest.of(0, 20);
        when(instanciaRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> comentarioService.listarPorInstancia(1, pageable));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Editar comentario confidencial - Éxito")
    void editar_Exito() {
        when(comentarioRepository.findById_IdComConfidencialAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(comentario));
        when(comentarioRepository.save(any(ComentarioConfidencialInstancia.class))).thenReturn(comentario);
        when(comentarioMapper.toDto(comentario)).thenReturn(responseDTO);

        ComentarioConfidencialInstanciaResponseDTO resultado =
                comentarioService.editar(1, 1, requestDTO);

        assertNotNull(resultado);
        verify(comentarioRepository).save(comentario);
    }

    @Test
    @DisplayName("Editar comentario confidencial - No encontrado (404)")
    void editar_LanzaNotFound() {
        when(comentarioRepository.findById_IdComConfidencialAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> comentarioService.editar(1, 99, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar comentario confidencial - Pertenece a otra instancia (404)")
    void editar_LanzaNotFound_PerteneceAOtraInstancia() {
        when(comentarioRepository.findById_IdComConfidencialAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(comentario));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> comentarioService.editar(2, 1, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dar de baja comentario confidencial - Éxito")
    void darDeBaja_Exito() {
        when(comentarioRepository.findById_IdComConfidencialAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(comentario));

        comentarioService.darDeBaja(1, 1);

        assertEquals(EstadoLogico.ELIMINADO, comentario.getEstado());
        verify(comentarioRepository).save(comentario);
    }

    @Test
    @DisplayName("Dar de baja comentario confidencial - No encontrado (404)")
    void darDeBaja_LanzaNotFound() {
        when(comentarioRepository.findById_IdComConfidencialAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> comentarioService.darDeBaja(1, 99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Dar de baja comentario confidencial - Pertenece a otra instancia (404, M14)")
    void darDeBaja_LanzaNotFound_PerteneceAOtraInstancia() {
        when(comentarioRepository.findById_IdComConfidencialAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(comentario));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> comentarioService.darDeBaja(2, 1));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(comentarioRepository, never()).save(any());
    }
}