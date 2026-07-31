package com.fides.sienep.service;

import com.fides.sienep.dto.IncidenciaRequestDTO;
import com.fides.sienep.dto.IncidenciaResponseDTO;
import com.fides.sienep.mapper.IncidenciaMapper;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.*;
import com.fides.sienep.utils.InstanciaIdGenerator;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidenciaServiceTest {

    @Mock private IncidenciaRepository incidenciaRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private InstanciaIdGenerator instanciaIdGenerator;
    @Mock private IncidenciaMapper incidenciaMapper;

    @InjectMocks
    private IncidenciaService incidenciaService;

    private Estudiante estudiante;
    private Funcionario funcionario;
    private Incidencia incidencia;
    private IncidenciaResponseDTO responseDTO;
    private IncidenciaRequestDTO requestDTO;

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

        incidencia = new Incidencia();
        incidencia.setCodInstancia(1);
        incidencia.setTitulo("Llegada tarde reiterada");
        incidencia.setFechaHora(LocalDateTime.now());
        incidencia.setEstudiante(estudiante);
        incidencia.setFuncionario(funcionario);
        incidencia.setLugar("Aula 3");
        incidencia.setEstado(EstadoLogico.ACTIVO);

        requestDTO = new IncidenciaRequestDTO();
        requestDTO.setTitulo("Llegada tarde reiterada");
        requestDTO.setFechaHora(LocalDateTime.now());
        requestDTO.setIdEstudiante(1);
        requestDTO.setCanal("Presencial");
        requestDTO.setLugar("Aula 3");

        responseDTO = new IncidenciaResponseDTO();
        responseDTO.setCodInstancia(1);
        responseDTO.setTitulo("Llegada tarde reiterada");
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
    @DisplayName("Crear incidencia - Éxito")
    void crear_Exito() {
        mockSecurityContext(2);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(instanciaIdGenerator.generarIdNeg()).thenReturn("INST-2026-000001");
        when(incidenciaRepository.save(any(Incidencia.class))).thenReturn(incidencia);
        when(incidenciaMapper.toDto(incidencia)).thenReturn(responseDTO);

        IncidenciaResponseDTO resultado = incidenciaService.crear(requestDTO);

        assertNotNull(resultado);
        assertEquals(1, resultado.getCodInstancia());
        verify(incidenciaRepository).save(any(Incidencia.class));
    }

    @Test
    @DisplayName("Crear incidencia - Estudiante no encontrado (404)")
    void crear_LanzaNotFound_EstudianteNoExiste() {
        mockSecurityContext(2);
        when(estudianteRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.crear(requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear incidencia - Estudiante inactivo (422)")
    void crear_LanzaUnprocessable_EstudianteInactivo() {
        mockSecurityContext(2);
        estudiante.setEstado(EstadoLogico.INACTIVO);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.crear(requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear incidencia - Funcionario inactivo (422)")
    void crear_LanzaUnprocessable_FuncionarioInactivo() {
        mockSecurityContext(2);
        funcionario.setEstado(EstadoLogico.INACTIVO);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.crear(requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Listar activas - Éxito, paginado")
    void listarActivas_Exito() {
        Pageable pageable = PageRequest.of(0, 20);
        when(incidenciaRepository.findByEstado(EstadoLogico.ACTIVO, pageable))
                .thenReturn(new PageImpl<>(List.of(incidencia), pageable, 1));
        when(incidenciaMapper.toDtoList(List.of(incidencia))).thenReturn(List.of(responseDTO));

        Page<IncidenciaResponseDTO> resultado = incidenciaService.listarActivas(pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(1, resultado.getContent().size());
    }

    @Test
    @DisplayName("Buscar por ID - Éxito")
    void buscarPorId_Exito() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));
        when(incidenciaMapper.toDto(incidencia)).thenReturn(responseDTO);

        IncidenciaResponseDTO resultado = incidenciaService.buscarPorId(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.getCodInstancia());
    }

    @Test
    @DisplayName("Buscar por ID - No encontrada (404)")
    void buscarPorId_LanzaNotFound() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.buscarPorId(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Actualizar incidencia - Éxito")
    void actualizar_Exito() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(incidenciaRepository.save(any(Incidencia.class))).thenReturn(incidencia);
        when(incidenciaMapper.toDto(incidencia)).thenReturn(responseDTO);

        IncidenciaResponseDTO resultado = incidenciaService.actualizar(1, requestDTO);

        assertNotNull(resultado);
        verify(incidenciaRepository).save(incidencia);
    }

    @Test
    @DisplayName("Actualizar incidencia - No encontrada (404)")
    void actualizar_LanzaNotFound() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.actualizar(99, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Desactivar - Éxito")
    void desactivar_Exito() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));

        incidenciaService.desactivar(1);

        assertEquals(EstadoLogico.INACTIVO, incidencia.getEstado());
        verify(incidenciaRepository).save(incidencia);
    }

    @Test
    @DisplayName("Reactivar - Éxito")
    void reactivar_Exito() {
        incidencia.setEstado(EstadoLogico.INACTIVO);
        when(incidenciaRepository.findById(1)).thenReturn(Optional.of(incidencia));
        when(incidenciaRepository.save(incidencia)).thenReturn(incidencia);
        when(incidenciaMapper.toDto(incidencia)).thenReturn(responseDTO);

        IncidenciaResponseDTO resultado = incidenciaService.reactivar(1);

        assertNotNull(resultado);
        assertEquals(EstadoLogico.ACTIVO, incidencia.getEstado());
        verify(incidenciaRepository).save(incidencia);
    }

    @Test
    @DisplayName("Reactivar - No encontrada (404)")
    void reactivar_LanzaNotFound() {
        when(incidenciaRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.reactivar(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactivar - Ya está activa (409)")
    void reactivar_LanzaConflict_YaActiva() {
        when(incidenciaRepository.findById(1)).thenReturn(Optional.of(incidencia));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.reactivar(1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Desactivar - No encontrada (404)")
    void desactivar_LanzaNotFound() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.desactivar(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Crear incidencia - Funcionario no encontrado (404)")
    void crear_LanzaNotFound_FuncionarioNoExiste() {
        mockSecurityContext(2);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.crear(requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar incidencia - Estudiante no encontrado (404)")
    void actualizar_LanzaNotFound_EstudianteNoExiste() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));
        when(estudianteRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.actualizar(1, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar incidencia - Estudiante inactivo (422)")
    void actualizar_LanzaUnprocessable_EstudianteInactivo() {
        estudiante.setEstado(EstadoLogico.INACTIVO);
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.actualizar(1, requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Listar por estudiante - Éxito, paginado")
    void listarPorEstudiante_Exito() {
        Pageable pageable = PageRequest.of(0, 20);
        when(incidenciaRepository.findByEstudianteAndEstado(1, EstadoLogico.ACTIVO, pageable))
                .thenReturn(new PageImpl<>(List.of(incidencia), pageable, 1));
        when(incidenciaMapper.toDtoList(List.of(incidencia))).thenReturn(List.of(responseDTO));

        Page<IncidenciaResponseDTO> resultado = incidenciaService.listarPorEstudiante(1, pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Listar por funcionario - Éxito, paginado")
    void listarPorFuncionario_Exito() {
        Pageable pageable = PageRequest.of(0, 20);
        when(incidenciaRepository.findByFuncionarioAndEstado(2, EstadoLogico.ACTIVO, pageable))
                .thenReturn(new PageImpl<>(List.of(incidencia), pageable, 1));
        when(incidenciaMapper.toDtoList(List.of(incidencia))).thenReturn(List.of(responseDTO));

        Page<IncidenciaResponseDTO> resultado = incidenciaService.listarPorFuncionario(2, pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Listar mis incidencias - Éxito")
    void listarMisIncidencias_Exito() {
        mockSecurityContext(1);
        when(incidenciaRepository.findByEstudianteAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(List.of(incidencia));
        when(incidenciaMapper.toDtoList(List.of(incidencia))).thenReturn(List.of(responseDTO));

        List<IncidenciaResponseDTO> resultado = incidenciaService.listarMisIncidencias();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }
}