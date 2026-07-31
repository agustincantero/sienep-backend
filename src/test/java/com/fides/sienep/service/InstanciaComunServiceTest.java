package com.fides.sienep.service;

import com.fides.sienep.dto.InstanciaComunRequestDTO;
import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.dto.InstanciaComunUpdateDTO;
import com.fides.sienep.dto.InstanciaDesdeRecordatorioDTO;
import com.fides.sienep.dto.RecordatorioInicialDTO;
import com.fides.sienep.dto.RecordatorioRequestDTO;
import com.fides.sienep.dto.RecordatorioResponseDTO;
import com.fides.sienep.mapper.InstanciaComunMapper;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.*;
import com.fides.sienep.utils.InstanciaIdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstanciaComunServiceTest {

    @Mock private InstanciaComunRepository instanciaComunRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private CategoriaInstanciaRepository categoriaInstanciaRepository;
    @Mock private InstanciaIdGenerator instanciaIdGenerator;
    @Mock private InstanciaComunMapper instanciaComunMapper;
    @Mock private EmailService emailService;
    @Mock private RecordatorioService recordatorioService;
    @Mock private RecordatorioRepository recordatorioRepository;

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
        funcionario.setEmail("juan.garcia@utec.edu.uy");
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
        responseDTO.setIdNegInstancia("INST-2026-000001");
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

    // ── Crear ─────────────────────────────────────────────────

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
    @DisplayName("Crear instancia común - Notificación enviada al funcionario creador (RF15)")
    void crear_EnviaNotificacionAlFuncionario() {
        mockSecurityContext(2);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(categoriaInstanciaRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(instanciaIdGenerator.generarIdNeg()).thenReturn("INST-2026-000001");
        when(instanciaComunRepository.save(any(InstanciaComun.class))).thenReturn(instancia);
        when(instanciaComunMapper.toDto(instancia)).thenReturn(responseDTO);

        instanciaComunService.crear(requestDTO);

        verify(emailService).enviarNotificacionInstancia(
                eq("juan.garcia@utec.edu.uy"),
                eq("Juan"),
                eq("INST-2026-000001"),
                eq("Reunión de seguimiento")
        );
    }

    @Test
    @DisplayName("Crear instancia común - Fallo de email no revierte la transacción (RF15)")
    void crear_FalloDeEmail_NoRollback() {
        mockSecurityContext(2);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(categoriaInstanciaRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(instanciaIdGenerator.generarIdNeg()).thenReturn("INST-2026-000001");
        when(instanciaComunRepository.save(any(InstanciaComun.class))).thenReturn(instancia);
        when(instanciaComunMapper.toDto(instancia)).thenReturn(responseDTO);
        doThrow(new RuntimeException("SMTP no disponible"))
                .when(emailService).enviarNotificacionInstancia(any(), any(), any(), any());

        InstanciaComunResponseDTO resultado = assertDoesNotThrow(
                () -> instanciaComunService.crear(requestDTO));

        assertNotNull(resultado);
        verify(instanciaComunRepository).save(any(InstanciaComun.class));
    }

    @Test
    @DisplayName("Crear instancia común - Con recordatorioInicial delega en RecordatorioService y devuelve ambos (RF13)")
    void crear_ConRecordatorioInicial_DelegaEnRecordatorioService() {
        RecordatorioInicialDTO recordatorioInicial = new RecordatorioInicialDTO();
        recordatorioInicial.setTitulo("Recordatorio de seguimiento");
        recordatorioInicial.setTipo(TipoRecordatorio.LLAMADA);
        recordatorioInicial.setFechaHora(LocalDateTime.now().plusDays(1));
        recordatorioInicial.setIdCategoria(1);
        requestDTO.setRecordatorioInicial(recordatorioInicial);

        RecordatorioResponseDTO recordatorioResponseDTO = new RecordatorioResponseDTO();
        recordatorioResponseDTO.setCodRecordatorio(1);
        recordatorioResponseDTO.setIdNegRecordatorio("REC-2026-000001");
        recordatorioResponseDTO.setCodInstancia(1);

        mockSecurityContext(2);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(categoriaInstanciaRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(instanciaIdGenerator.generarIdNeg()).thenReturn("INST-2026-000001");
        when(instanciaComunRepository.save(any(InstanciaComun.class))).thenReturn(instancia);
        when(instanciaComunMapper.toDto(instancia)).thenReturn(responseDTO);
        when(recordatorioService.crear(any(RecordatorioRequestDTO.class))).thenReturn(recordatorioResponseDTO);

        InstanciaComunResponseDTO resultado = instanciaComunService.crear(requestDTO);

        assertNotNull(resultado.getRecordatorioCreado());
        assertEquals("REC-2026-000001", resultado.getRecordatorioCreado().getIdNegRecordatorio());

        ArgumentCaptor<RecordatorioRequestDTO> captor = ArgumentCaptor.forClass(RecordatorioRequestDTO.class);
        verify(recordatorioService).crear(captor.capture());

        RecordatorioRequestDTO enviado = captor.getValue();
        assertEquals(1, enviado.getCodInstancia());
        assertEquals("Recordatorio de seguimiento", enviado.getTitulo());
        // idResponsable no vino en el recordatorioInicial → debe caer al funcionario creador (idUsuario 2)
        assertEquals(2, enviado.getIdResponsable());
    }

    @Test
    @DisplayName("Crear instancia común - recordatorioInicial con nroOcurrencias=0 lanza 422 (regresión RF13)")
    void crear_ConRecordatorioInicialNroOcurrenciasCero_LanzaUnprocessable() {
        RecordatorioInicialDTO recordatorioInicial = new RecordatorioInicialDTO();
        recordatorioInicial.setTitulo("Recordatorio de seguimiento");
        recordatorioInicial.setTipo(TipoRecordatorio.LLAMADA);
        recordatorioInicial.setFechaHora(LocalDateTime.now().plusDays(1));
        recordatorioInicial.setIdCategoria(1);
        recordatorioInicial.setNroOcurrencias(0);
        requestDTO.setRecordatorioInicial(recordatorioInicial);

        mockSecurityContext(2);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(categoriaInstanciaRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(instanciaIdGenerator.generarIdNeg()).thenReturn("INST-2026-000001");
        when(instanciaComunRepository.save(any(InstanciaComun.class))).thenReturn(instancia);
        when(instanciaComunMapper.toDto(instancia)).thenReturn(responseDTO);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.crear(requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(recordatorioService, never()).crear(any());
    }

    @Test
    @DisplayName("Crear instancia común - Sin recordatorioInicial no llama a RecordatorioService (RF13)")
    void crear_SinRecordatorioInicial_NoLlamaARecordatorioService() {
        mockSecurityContext(2);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(categoriaInstanciaRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(instanciaIdGenerator.generarIdNeg()).thenReturn("INST-2026-000001");
        when(instanciaComunRepository.save(any(InstanciaComun.class))).thenReturn(instancia);
        when(instanciaComunMapper.toDto(instancia)).thenReturn(responseDTO);

        InstanciaComunResponseDTO resultado = instanciaComunService.crear(requestDTO);

        assertNull(resultado.getRecordatorioCreado());
        verify(recordatorioService, never()).crear(any());
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

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(instanciaComunRepository, never()).save(any());
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
    @DisplayName("Crear instancia común - Funcionario inactivo (422)")
    void crear_LanzaUnprocessable_FuncionarioInactivo() {
        mockSecurityContext(2);
        funcionario.setEstado(EstadoLogico.INACTIVO);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.crear(requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
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
    @DisplayName("Crear instancia común - Categoría inactiva (422)")
    void crear_LanzaUnprocessable_CategoriaInactiva() {
        mockSecurityContext(2);
        categoria.setEstado(EstadoLogico.INACTIVO);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(categoriaInstanciaRepository.findById(1)).thenReturn(Optional.of(categoria));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.crear(requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(instanciaComunRepository, never()).save(any());
    }

    // ── Crear desde recordatorio (RF27) ──────────────────────

    @Test
    @DisplayName("Crear desde recordatorio - Éxito, deriva estudiante y marca cumplido")
    void crearDesdeRecordatorio_Exito() {
        Recordatorio recordatorio = new Recordatorio();
        recordatorio.setCodRecordatorio(5);
        recordatorio.setIdNegRecordatorio("REC-2026-000005");
        recordatorio.setTitulo("Llamar a la familia");
        recordatorio.setInstancia(instancia); // instancia.getEstudiante() == estudiante (fixture)
        recordatorio.setCumplido(false);

        InstanciaDesdeRecordatorioDTO dto = new InstanciaDesdeRecordatorioDTO();
        dto.setIdCategoria(1);

        mockSecurityContext(2);
        when(recordatorioRepository.findByCodRecordatorioAndEstadoParaActualizar(5, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(categoriaInstanciaRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(instanciaIdGenerator.generarIdNeg()).thenReturn("INST-2026-000002");
        when(instanciaComunRepository.save(any(InstanciaComun.class))).thenReturn(instancia);
        when(instanciaComunMapper.toDto(instancia)).thenReturn(responseDTO);

        InstanciaComunResponseDTO resultado = instanciaComunService.crearDesdeRecordatorio(5, dto);

        assertNotNull(resultado);

        ArgumentCaptor<InstanciaComun> captor = ArgumentCaptor.forClass(InstanciaComun.class);
        verify(instanciaComunRepository).save(captor.capture());
        assertEquals("Seguimiento: Llamar a la familia", captor.getValue().getTitulo());
        assertEquals(1, captor.getValue().getEstudiante().getIdUsuario());

        verify(recordatorioService).marcarCumplido(5);
    }

    @Test
    @DisplayName("Crear desde recordatorio - Con título propio no usa el default")
    void crearDesdeRecordatorio_ConTituloPropio_NoUsaDefault() {
        Recordatorio recordatorio = new Recordatorio();
        recordatorio.setCodRecordatorio(5);
        recordatorio.setTitulo("Llamar a la familia");
        recordatorio.setInstancia(instancia);
        recordatorio.setCumplido(false);

        InstanciaDesdeRecordatorioDTO dto = new InstanciaDesdeRecordatorioDTO();
        dto.setIdCategoria(1);
        dto.setTitulo("Seguimiento post-llamada");

        mockSecurityContext(2);
        when(recordatorioRepository.findByCodRecordatorioAndEstadoParaActualizar(5, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(categoriaInstanciaRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(instanciaIdGenerator.generarIdNeg()).thenReturn("INST-2026-000002");
        when(instanciaComunRepository.save(any(InstanciaComun.class))).thenReturn(instancia);
        when(instanciaComunMapper.toDto(instancia)).thenReturn(responseDTO);

        instanciaComunService.crearDesdeRecordatorio(5, dto);

        ArgumentCaptor<InstanciaComun> captor = ArgumentCaptor.forClass(InstanciaComun.class);
        verify(instanciaComunRepository).save(captor.capture());
        assertEquals("Seguimiento post-llamada", captor.getValue().getTitulo());
    }

    @Test
    @DisplayName("Crear desde recordatorio - Recordatorio no encontrado (404)")
    void crearDesdeRecordatorio_LanzaNotFound_RecordatorioNoExiste() {
        when(recordatorioRepository.findByCodRecordatorioAndEstadoParaActualizar(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.crearDesdeRecordatorio(99, new InstanciaDesdeRecordatorioDTO()));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(instanciaComunRepository, never()).save(any());
        verify(recordatorioService, never()).marcarCumplido(anyInt());
    }

    @Test
    @DisplayName("Crear desde recordatorio - Ya cumplido (409)")
    void crearDesdeRecordatorio_LanzaConflict_YaCumplido() {
        Recordatorio recordatorio = new Recordatorio();
        recordatorio.setCodRecordatorio(5);
        recordatorio.setInstancia(instancia);
        recordatorio.setCumplido(true);

        when(recordatorioRepository.findByCodRecordatorioAndEstadoParaActualizar(5, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.crearDesdeRecordatorio(5, new InstanciaDesdeRecordatorioDTO()));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(instanciaComunRepository, never()).save(any());
        verify(recordatorioService, never()).marcarCumplido(anyInt());
    }

    // ── Listar ────────────────────────────────────────────────

    @Test
    @DisplayName("Listar activas - Éxito, paginado (H3)")
    void listarActivas_Exito() {
        Pageable pageable = PageRequest.of(0, 20);
        when(instanciaComunRepository.findByEstado(EstadoLogico.ACTIVO, pageable))
                .thenReturn(new PageImpl<>(List.of(instancia), pageable, 1));
        when(instanciaComunMapper.toDtoList(List.of(instancia))).thenReturn(List.of(responseDTO));

        Page<InstanciaComunResponseDTO> resultado = instanciaComunService.listarActivas(pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Listar por estudiante - Éxito, paginado (H3)")
    void listarPorEstudiante_Exito() {
        Pageable pageable = PageRequest.of(0, 20);
        when(instanciaComunRepository.findByEstudianteAndEstado(1, EstadoLogico.ACTIVO, pageable))
                .thenReturn(new PageImpl<>(List.of(instancia), pageable, 1));
        when(instanciaComunMapper.toDtoList(List.of(instancia))).thenReturn(List.of(responseDTO));

        Page<InstanciaComunResponseDTO> resultado = instanciaComunService.listarPorEstudiante(1, pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Listar por funcionario - Éxito, paginado (H3)")
    void listarPorFuncionario_Exito() {
        Pageable pageable = PageRequest.of(0, 20);
        when(instanciaComunRepository.findByFuncionarioAndEstado(2, EstadoLogico.ACTIVO, pageable))
                .thenReturn(new PageImpl<>(List.of(instancia), pageable, 1));
        when(instanciaComunMapper.toDtoList(List.of(instancia))).thenReturn(List.of(responseDTO));

        Page<InstanciaComunResponseDTO> resultado = instanciaComunService.listarPorFuncionario(2, pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Listar por categoría - Éxito, paginado (H3)")
    void listarPorCategoria_Exito() {
        Pageable pageable = PageRequest.of(0, 20);
        when(instanciaComunRepository.findByCategoriaAndEstado(1, EstadoLogico.ACTIVO, pageable))
                .thenReturn(new PageImpl<>(List.of(instancia), pageable, 1));
        when(instanciaComunMapper.toDtoList(List.of(instancia))).thenReturn(List.of(responseDTO));

        Page<InstanciaComunResponseDTO> resultado = instanciaComunService.listarPorCategoria(1, pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
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

    // ── Buscar por ID ─────────────────────────────────────────

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

    // ── Actualizar ────────────────────────────────────────────

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

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(instanciaComunRepository, never()).save(any());
    }

    // ── Dar de baja ───────────────────────────────────────────

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

    // ── Reactivar (H8) ────────────────────────────────────────

    @Test
    @DisplayName("Reactivar - Éxito (H8)")
    void reactivar_Exito() {
        instancia.setEstado(EstadoLogico.INACTIVO);
        when(instanciaComunRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(instanciaComunRepository.save(instancia)).thenReturn(instancia);
        when(instanciaComunMapper.toDto(instancia)).thenReturn(responseDTO);

        InstanciaComunResponseDTO resultado = instanciaComunService.reactivar(1);

        assertNotNull(resultado);
        assertEquals(EstadoLogico.ACTIVO, instancia.getEstado());
        verify(instanciaComunRepository).save(instancia);
    }

    @Test
    @DisplayName("Reactivar - No encontrada (404) (H8)")
    void reactivar_LanzaNotFound() {
        when(instanciaComunRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.reactivar(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(instanciaComunRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactivar - Ya está activa (409) (H8)")
    void reactivar_LanzaConflict_YaActiva() {
        when(instanciaComunRepository.findById(1)).thenReturn(Optional.of(instancia));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> instanciaComunService.reactivar(1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(instanciaComunRepository, never()).save(any());
    }
}