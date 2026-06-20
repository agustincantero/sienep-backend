package com.fides.sienep.service;

import com.fides.sienep.dto.InstanciaComunRequestDTO;
import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.dto.InstanciaComunUpdateDTO;
import com.fides.sienep.mapper.InstanciaComunMapper;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.*;
import com.fides.sienep.utils.InstanciaIdGenerator;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstanciaComunServiceTest {

    @Mock private InstanciaComunRepository instanciaComunRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private CategoriaInstanciaRepository categoriaInstanciaRepository;
    @Mock private InstanciaIdGenerator instanciaIdGenerator;
    @Mock private InstanciaComunMapper instanciaComunMapper;

    @InjectMocks
    private InstanciaComunService instanciaComunService;

    private Estudiante estudiante;
    private Funcionario funcionario;
    private CategoriaInstancia categoria;
    private InstanciaComun instancia;
    private InstanciaComunResponseDTO responseDTO;
    private InstanciaComunRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        estudiante = new Estudiante();
        estudiante.setIdUsuario(1);
        estudiante.setNombre("Ana");
        estudiante.setApellido("Pérez");
        estudiante.setEstado(EstadoLogico.ACTIVO);

        funcionario = new Funcionario();
        funcionario.setIdUsuario(2);
        funcionario.setNombre("Juan");
        funcionario.setApellido("García");
        funcionario.setEstado(EstadoLogico.ACTIVO);

        categoria = new CategoriaInstancia();
        categoria.setIdCategoria(1);
        categoria.setNomCategoria("REUNIÓN");
        categoria.setEstado(EstadoLogico.ACTIVO);

        instancia = new InstanciaComun();
        instancia.setCodInstancia(1);
        instancia.setTitulo("Reunión de seguimiento");
        instancia.setFechaHora(LocalDateTime.now());
        instancia.setEstudiante(estudiante);
        instancia.setFuncionario(funcionario);
        instancia.setCategoria(categoria);
        instancia.setEstado(EstadoLogico.ACTIVO);

        requestDTO = new InstanciaComunRequestDTO();
        requestDTO.setTitulo("Reunión de seguimiento");
        requestDTO.setFechaHora(LocalDateTime.now());
        requestDTO.setIdEstudiante(1);
        requestDTO.setIdCategoria(1);

        responseDTO = new InstanciaComunResponseDTO();
        responseDTO.setCodInstancia(1);
        responseDTO.setTitulo("Reunión de seguimiento");
        responseDTO.setEstado(EstadoLogico.ACTIVO);
    }

    private void mockSecurityContext(int idUsuario) {
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(auth.getName()).thenReturn(String.valueOf(idUsuario));
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Crear instancia común - Éxito")
    void crear_Exito() {
        mockSecurityContext(2);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(categoriaInstanciaRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(instanciaIdGenerator.generarIdNeg()).thenReturn("INST-2026-000001");
        when(instanciaComunRepository.save(any(InstanciaComun.class))).thenReturn(instancia);
        when(instanciaComunMapper.toDto(instancia)).thenReturn(responseDTO);

        InstanciaComunResponseDTO resultado = instanciaComunService.crear(requestDTO);

        assertNotNull(resultado);
        assertEquals(1, resultado.getCodInstancia());
        verify(instanciaComunRepository).save(any(InstanciaComun.class));
    }

    @Test
    @DisplayName("Crear instancia común - Estudiante no encontrado (404)")
    void crear_LanzaNotFound_EstudianteNoExiste() {
        mockSecurityContext(2);
        when(estudianteRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.crear(requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(instanciaComunRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear instancia común - Estudiante inactivo (422)")
    void crear_LanzaUnprocessable_EstudianteInactivo() {
        mockSecurityContext(2);
        estudiante.setEstado(EstadoLogico.INACTIVO);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.crear(requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(instanciaComunRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear instancia común - Funcionario inactivo (422)")
    void crear_LanzaUnprocessable_FuncionarioInactivo() {
        mockSecurityContext(2);
        funcionario.setEstado(EstadoLogico.INACTIVO);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.crear(requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(instanciaComunRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear instancia común - Categoría inactiva (422)")
    void crear_LanzaUnprocessable_CategoriaInactiva() {
        mockSecurityContext(2);
        categoria.setEstado(EstadoLogico.INACTIVO);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(categoriaInstanciaRepository.findById(1)).thenReturn(Optional.of(categoria));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.crear(requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(instanciaComunRepository, never()).save(any());
    }

    @Test
    @DisplayName("Listar activas - Éxito")
    void listarActivas_Exito() {
        when(instanciaComunRepository.findByEstado(EstadoLogico.ACTIVO)).thenReturn(List.of(instancia));
        when(instanciaComunMapper.toDtoList(List.of(instancia))).thenReturn(List.of(responseDTO));

        List<InstanciaComunResponseDTO> resultado = instanciaComunService.listarActivas();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Buscar por ID - Éxito")
    void buscarPorId_Exito() {
        when(instanciaComunRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(instancia));
        when(instanciaComunMapper.toDto(instancia)).thenReturn(responseDTO);

        InstanciaComunResponseDTO resultado = instanciaComunService.buscarPorId(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.getCodInstancia());
    }

    @Test
    @DisplayName("Buscar por ID - No encontrada (404)")
    void buscarPorId_LanzaNotFound() {
        when(instanciaComunRepository.findByCodInstanciaAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.buscarPorId(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Actualizar instancia común - Éxito")
    void actualizar_Exito() {
        InstanciaComunUpdateDTO updateDTO = new InstanciaComunUpdateDTO();
        updateDTO.setTitulo("Nuevo título");
        updateDTO.setFechaHora(LocalDateTime.now());
        updateDTO.setIdCategoria(1);

        when(instanciaComunRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(instancia));
        when(categoriaInstanciaRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(instanciaComunRepository.save(any(InstanciaComun.class))).thenReturn(instancia);
        when(instanciaComunMapper.toDto(instancia)).thenReturn(responseDTO);

        InstanciaComunResponseDTO resultado = instanciaComunService.actualizar(1, updateDTO);

        assertNotNull(resultado);
        verify(instanciaComunRepository).save(instancia);
    }

    @Test
    @DisplayName("Dar de baja - Éxito")
    void darDeBaja_Exito() {
        when(instanciaComunRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(instancia));

        instanciaComunService.darDeBaja(1);

        assertEquals(EstadoLogico.INACTIVO, instancia.getEstado());
        verify(instanciaComunRepository).save(instancia);
    }

    @Test
    @DisplayName("Dar de baja - No encontrada (404)")
    void darDeBaja_LanzaNotFound() {
        when(instanciaComunRepository.findByCodInstanciaAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.darDeBaja(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Crear instancia común - Funcionario no encontrado (404)")
    void crear_LanzaNotFound_FuncionarioNoExiste() {
        mockSecurityContext(2);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.crear(requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(instanciaComunRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear instancia común - Categoría no encontrada (404)")
    void crear_LanzaNotFound_CategoriaNoExiste() {
        mockSecurityContext(2);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(categoriaInstanciaRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.crear(requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(instanciaComunRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar instancia común - Categoría no encontrada (404)")
    void actualizar_LanzaNotFound_CategoriaNoExiste() {
        InstanciaComunUpdateDTO updateDTO = new InstanciaComunUpdateDTO();
        updateDTO.setTitulo("Nuevo título");
        updateDTO.setFechaHora(LocalDateTime.now());
        updateDTO.setIdCategoria(99);

        when(instanciaComunRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(instancia));
        when(categoriaInstanciaRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.actualizar(1, updateDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(instanciaComunRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar instancia común - Categoría inactiva (422)")
    void actualizar_LanzaUnprocessable_CategoriaInactiva() {
        InstanciaComunUpdateDTO updateDTO = new InstanciaComunUpdateDTO();
        updateDTO.setTitulo("Nuevo título");
        updateDTO.setFechaHora(LocalDateTime.now());
        updateDTO.setIdCategoria(1);

        categoria.setEstado(EstadoLogico.INACTIVO);
        when(instanciaComunRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(instancia));
        when(categoriaInstanciaRepository.findById(1)).thenReturn(Optional.of(categoria));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.actualizar(1, updateDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(instanciaComunRepository, never()).save(any());
    }

    @Test
    @DisplayName("Listar por estudiante - Éxito")
    void listarPorEstudiante_Exito() {
        when(instanciaComunRepository.findByEstudianteAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(List.of(instancia));
        when(instanciaComunMapper.toDtoList(List.of(instancia))).thenReturn(List.of(responseDTO));

        List<InstanciaComunResponseDTO> resultado = instanciaComunService.listarPorEstudiante(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Listar por funcionario - Éxito")
    void listarPorFuncionario_Exito() {
        when(instanciaComunRepository.findByFuncionarioAndEstado(2, EstadoLogico.ACTIVO))
                .thenReturn(List.of(instancia));
        when(instanciaComunMapper.toDtoList(List.of(instancia))).thenReturn(List.of(responseDTO));

        List<InstanciaComunResponseDTO> resultado = instanciaComunService.listarPorFuncionario(2);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Listar por categoría - Éxito")
    void listarPorCategoria_Exito() {
        when(instanciaComunRepository.findByCategoriaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(List.of(instancia));
        when(instanciaComunMapper.toDtoList(List.of(instancia))).thenReturn(List.of(responseDTO));

        List<InstanciaComunResponseDTO> resultado = instanciaComunService.listarPorCategoria(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Listar mis instancias - Éxito")
    void listarMisInstancias_Exito() {
        mockSecurityContext(1);
        when(instanciaComunRepository.findByEstudianteAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(List.of(instancia));
        when(instanciaComunMapper.toDtoList(List.of(instancia))).thenReturn(List.of(responseDTO));

        List<InstanciaComunResponseDTO> resultado = instanciaComunService.listarMisInstancias();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }
}