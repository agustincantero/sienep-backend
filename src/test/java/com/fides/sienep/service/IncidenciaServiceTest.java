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
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("Crear incidencia - Fecha en el futuro (422), no consulta nada")
    void crear_FechaFutura_LanzaUnprocessable() {
        requestDTO.setFechaHora(LocalDateTime.now().plusMinutes(5));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.crear(requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        assertEquals("La fecha y hora de la incidencia no puede estar en el futuro", ex.getReason());
        verifyNoInteractions(estudianteRepository, incidenciaRepository);
    }

    @Test
    @DisplayName("Crear incidencia - Fecha en el pasado es válida")
    void crear_FechaPasada_Exito() {
        mockSecurityContext(2);
        requestDTO.setFechaHora(LocalDateTime.now().minusDays(2));
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(instanciaIdGenerator.generarIdNeg()).thenReturn("INST-2026-000001");
        when(incidenciaRepository.save(any(Incidencia.class))).thenReturn(incidencia);
        when(incidenciaMapper.toDto(incidencia)).thenReturn(responseDTO);

        assertNotNull(incidenciaService.crear(requestDTO));
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

    // Qué devuelve cada combinación de filtros lo verifica IncidenciaBusquedaIT contra Postgres: con Mockito solo se
    // puede comprobar que el service arma la consulta y devuelve la página mapeada.
    @Test
    @DisplayName("Listar - Sin filtros ni estado, devuelve la página mapeada")
    void listar_SinFiltros_DevuelvePaginaMapeada() {
        Pageable pageable = PageRequest.of(0, 20);
        when(incidenciaRepository.findAll(ArgumentMatchers.<Specification<Incidencia>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(incidencia), pageable, 1));
        when(incidenciaMapper.toDtoList(List.of(incidencia))).thenReturn(List.of(responseDTO));

        Page<IncidenciaResponseDTO> resultado = incidenciaService.listar(null, null, null, pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(List.of(responseDTO), resultado.getContent());
    }

    @Test
    @DisplayName("Listar - Con todos los filtros a la vez, hace una sola consulta paginada")
    void listar_ConTodosLosFiltros_HaceUnaSolaConsulta() {
        Pageable pageable = PageRequest.of(1, 10);
        when(incidenciaRepository.findAll(ArgumentMatchers.<Specification<Incidencia>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(incidencia), pageable, 11));
        when(incidenciaMapper.toDtoList(List.of(incidencia))).thenReturn(List.of(responseDTO));

        Page<IncidenciaResponseDTO> resultado = incidenciaService.listar(1, 2, List.of(EstadoLogico.INACTIVO), pageable);

        assertEquals(11, resultado.getTotalElements());
        verify(incidenciaRepository, times(1))
                .findAll(ArgumentMatchers.<Specification<Incidencia>>any(), eq(pageable));
        verifyNoMoreInteractions(incidenciaRepository);
    }

    @Test
    @DisplayName("Buscar por ID - Éxito")
    void obtenerPorId_Exito() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));
        when(incidenciaMapper.toDto(incidencia)).thenReturn(responseDTO);

        IncidenciaResponseDTO resultado = incidenciaService.obtenerPorId(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.getCodInstancia());
    }

    @Test
    @DisplayName("Buscar por ID - No encontrada (404)")
    void obtenerPorId_LanzaNotFound() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.obtenerPorId(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Editar incidencia - Éxito")
    void editar_Exito() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(incidenciaRepository.save(any(Incidencia.class))).thenReturn(incidencia);
        when(incidenciaMapper.toDto(incidencia)).thenReturn(responseDTO);

        IncidenciaResponseDTO resultado = incidenciaService.editar(1, requestDTO);

        assertNotNull(resultado);
        verify(incidenciaRepository).save(incidencia);
    }

    @Test
    @DisplayName("Editar incidencia - Cambiar la fecha a una futura (422)")
    void editar_FechaNuevaEnElFuturo_LanzaUnprocessable() {
        requestDTO.setFechaHora(LocalDateTime.now().plusDays(1));
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.editar(1, requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        assertEquals("La fecha y hora de la incidencia no puede estar en el futuro", ex.getReason());
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar incidencia - Sin cambiar la fecha se puede editar el resto, aunque la guardada fuera futura")
    void editar_FechaSinCambios_Exito() {
        LocalDateTime fechaGuardada = LocalDateTime.now().plusDays(1).withSecond(42);
        incidencia.setFechaHora(fechaGuardada);
        // El front manda la fecha sin segundos: igual cuenta como "sin cambios".
        requestDTO.setFechaHora(fechaGuardada.truncatedTo(ChronoUnit.MINUTES));
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(incidenciaRepository.save(any(Incidencia.class))).thenReturn(incidencia);
        when(incidenciaMapper.toDto(incidencia)).thenReturn(responseDTO);

        assertNotNull(incidenciaService.editar(1, requestDTO));
        verify(incidenciaRepository).save(incidencia);
    }

    @Test
    @DisplayName("Editar incidencia - No encontrada (404)")
    void editar_LanzaNotFound() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.editar(99, requestDTO));

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
    @DisplayName("Editar incidencia - Estudiante no encontrado (404)")
    void editar_LanzaNotFound_EstudianteNoExiste() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));
        when(estudianteRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.editar(1, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar incidencia - Estudiante inactivo (422)")
    void editar_LanzaUnprocessable_EstudianteInactivo() {
        estudiante.setEstado(EstadoLogico.INACTIVO);
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> incidenciaService.editar(1, requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(incidenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Listar mis incidencias - Éxito")
    void listarMisIncidencias_Exito() {
        mockSecurityContext(1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Incidencia> pagina = new PageImpl<>(List.of(incidencia), pageable, 1);

        when(incidenciaRepository.findByEstudianteAndEstado(1, EstadoLogico.ACTIVO, pageable))
                .thenReturn(pagina);
        when(incidenciaMapper.toDtoList(List.of(incidencia))).thenReturn(List.of(responseDTO));

        Page<IncidenciaResponseDTO> resultado = incidenciaService.listarMisIncidencias(pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Listar mis incidencias - El estudiante ve su documento pero no el del funcionario")
    void listarMisIncidencias_OcultaElDocumentoDelFuncionario() {
        mockSecurityContext(1);
        Pageable pageable = PageRequest.of(0, 10);
        responseDTO.setDocumentoEstudiante("43867154");
        responseDTO.setDocumentoFuncionario("11111111");

        when(incidenciaRepository.findByEstudianteAndEstado(1, EstadoLogico.ACTIVO, pageable))
                .thenReturn(new PageImpl<>(List.of(incidencia), pageable, 1));
        when(incidenciaMapper.toDtoList(List.of(incidencia))).thenReturn(List.of(responseDTO));

        IncidenciaResponseDTO dto = incidenciaService.listarMisIncidencias(pageable).getContent().get(0);

        assertEquals("43867154", dto.getDocumentoEstudiante());
        assertNull(dto.getDocumentoFuncionario());
    }
}