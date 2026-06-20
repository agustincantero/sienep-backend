package com.fides.sienep.service;

import com.fides.sienep.dto.IncidenciaRequestDTO;
import com.fides.sienep.dto.IncidenciaResponseDTO;
import com.fides.sienep.mapper.IncidenciaMapper;
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

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
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

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Listar activas - Éxito")
    void listarActivas_Exito() {
        when(incidenciaRepository.findByEstado(EstadoLogico.ACTIVO)).thenReturn(List.of(incidencia));
        when(incidenciaMapper.toDtoList(List.of(incidencia))).thenReturn(List.of(responseDTO));

        List<IncidenciaResponseDTO> resultado = incidenciaService.listarActivas();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
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
    @DisplayName("Dar de baja - Éxito")
    void darDeBaja_Exito() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));

        incidenciaService.darDeBaja(1);

        assertEquals(EstadoLogico.INACTIVO, incidencia.getEstado());
        verify(incidenciaRepository).save(incidencia);
    }

    @Test
    @DisplayName("Dar de baja - No encontrada (404)")
    void darDeBaja_LanzaNotFound() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.darDeBaja(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}