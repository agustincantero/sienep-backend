package com.fides.sienep.service;

import com.fides.sienep.dto.RecordatorioRequestDTO;
import com.fides.sienep.dto.RecordatorioResponseDTO;
import com.fides.sienep.mapper.RecordatorioMapper;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.*;
import com.fides.sienep.utils.RecordatorioIdGenerator;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordatorioServiceTest {

    @Mock private RecordatorioRepository recordatorioRepository;
    @Mock private InstanciaRepository instanciaRepository;
    @Mock private CategoriaRecordatorioRepository categoriaRecordatorioRepository;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private FrecuenciaRepository frecuenciaRepository;
    @Mock private RecordatorioMapper recordatorioMapper;
    @Mock private RecordatorioIdGenerator recordatorioIdGenerator;
    @Mock private EmailService emailService;
    @Mock private FuncionarioGcalRepository funcionarioGcalRepository;
    @Mock private GoogleCalendarService googleCalendarService;

    @InjectMocks
    private RecordatorioService recordatorioService;

    private Instancia instancia;
    private CategoriaRecordatorio categoria;
    private Funcionario responsable;
    private Recordatorio recordatorio;
    private RecordatorioResponseDTO responseDTO;
    private RecordatorioRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        instancia = mock(Instancia.class);
        lenient().when(instancia.getCodInstancia()).thenReturn(1);
        lenient().when(instancia.getTitulo()).thenReturn("Reunión de seguimiento");
        lenient().when(instancia.getEstado()).thenReturn(EstadoLogico.ACTIVO);

        categoria = new CategoriaRecordatorio();
        categoria.setIdCategoria(1);
        categoria.setNomCategoria("SEGUIMIENTO");
        categoria.setEstado(EstadoLogico.ACTIVO);

        responsable = new Funcionario();
        responsable.setIdUsuario(2);
        responsable.setNombre("Juan");
        responsable.setApellido("García");
        responsable.setEmail("juan.garcia@utec.edu.uy");
        responsable.setEstado(EstadoLogico.ACTIVO);

        recordatorio = new Recordatorio();
        recordatorio.setCodRecordatorio(1);
        recordatorio.setTitulo("Llamar a Ana");
        recordatorio.setTipo(TipoRecordatorio.LLAMADA);
        recordatorio.setFechaHora(LocalDateTime.now().plusDays(1));
        recordatorio.setInstancia(instancia);
        recordatorio.setCategoria(categoria);
        recordatorio.setResponsable(responsable);
        recordatorio.setEstado(EstadoLogico.ACTIVO);
        recordatorio.setEstadoOperativo(EstadoRecordatorio.FUTURO);

        requestDTO = new RecordatorioRequestDTO();
        requestDTO.setTitulo("Llamar a Ana");
        requestDTO.setTipo(TipoRecordatorio.LLAMADA);
        requestDTO.setFechaHora(LocalDateTime.now().plusDays(1));
        requestDTO.setCodInstancia(1);
        requestDTO.setIdCategoria(1);
        requestDTO.setIdResponsable(2);

        responseDTO = new RecordatorioResponseDTO();
        responseDTO.setCodRecordatorio(1);
        responseDTO.setTitulo("Llamar a Ana");
        responseDTO.setEstado(EstadoLogico.ACTIVO);
    }

    @Test
    @DisplayName("Crear recordatorio - Éxito")
    void crear_Exito() {
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(responsable));
        when(recordatorioIdGenerator.generarIdNeg()).thenReturn("REC-2026-000001");
        when(recordatorioRepository.save(any(Recordatorio.class))).thenReturn(recordatorio);
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);

        RecordatorioResponseDTO resultado = recordatorioService.crear(requestDTO);

        assertNotNull(resultado);
        assertEquals(1, resultado.getCodRecordatorio());
        verify(recordatorioRepository).save(any(Recordatorio.class));
    }

    @Test
    @DisplayName("Crear recordatorio - Notificación enviada al responsable (RF23)")
    void crear_EnviaNotificacionAlResponsable() {
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(responsable));
        when(recordatorioIdGenerator.generarIdNeg()).thenReturn("REC-2026-000001");
        when(recordatorioRepository.save(any(Recordatorio.class))).thenReturn(recordatorio);
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);

        recordatorioService.crear(requestDTO);

        verify(emailService).enviarNotificacionRecordatorio(
                eq("juan.garcia@utec.edu.uy"),
                eq("Juan"),
                eq(recordatorio.getIdNegRecordatorio()),
                eq("Llamar a Ana"),
                eq(recordatorio.getFechaHora())
        );
    }

    @Test
    @DisplayName("Crear recordatorio - Fallo de email no revierte la transacción (RF23)")
    void crear_FalloDeEmail_NoRollback() {
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(responsable));
        when(recordatorioIdGenerator.generarIdNeg()).thenReturn("REC-2026-000001");
        when(recordatorioRepository.save(any(Recordatorio.class))).thenReturn(recordatorio);
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);
        doThrow(new RuntimeException("SMTP no disponible"))
                .when(emailService).enviarNotificacionRecordatorio(any(), any(), any(), any(), any());

        RecordatorioResponseDTO resultado = assertDoesNotThrow(
                () -> recordatorioService.crear(requestDTO));

        assertNotNull(resultado);
        verify(recordatorioRepository).save(any(Recordatorio.class));
    }

    @Test
    @DisplayName("Crear recordatorio - Evento creado en Google pero falla el guardado local del eventIdGoogle, no propaga")
    void crear_FallaGuardadoLocalEventIdGoogle_NoPropaga() {
        FuncionarioGcal gcal = new FuncionarioGcal(responsable, "refresh-token", "juan@gmail.com");

        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(responsable));
        when(recordatorioIdGenerator.generarIdNeg()).thenReturn("REC-2026-000001");
        when(recordatorioRepository.save(any(Recordatorio.class)))
                .thenReturn(recordatorio)
                .thenThrow(new RuntimeException("DB caída"));
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);
        when(funcionarioGcalRepository.findByFuncionarioIdUsuario(2)).thenReturn(Optional.of(gcal));
        when(googleCalendarService.crearEvento(any(), eq("refresh-token"))).thenReturn("evento-nuevo-id");

        RecordatorioResponseDTO resultado = assertDoesNotThrow(() -> recordatorioService.crear(requestDTO));

        assertNotNull(resultado);
        verify(recordatorioRepository, times(2)).save(any(Recordatorio.class));
    }

    @Test
    @DisplayName("Crear recordatorio - Instancia no encontrada (404)")
    void crear_LanzaNotFound_InstanciaNoExiste() {
        when(instanciaRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.crear(requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear recordatorio - Instancia inactiva (422)")
    void crear_LanzaUnprocessable_InstanciaInactiva() {
        when(instancia.getEstado()).thenReturn(EstadoLogico.INACTIVO);
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.crear(requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear recordatorio - Categoría no encontrada (404)")
    void crear_LanzaNotFound_CategoriaNoExiste() {
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.crear(requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear recordatorio - Categoría inactiva (422)")
    void crear_LanzaUnprocessable_CategoriaInactiva() {
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        categoria.setEstado(EstadoLogico.INACTIVO);
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.crear(requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear recordatorio - Responsable no encontrado (404)")
    void crear_LanzaNotFound_ResponsableNoExiste() {
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.crear(requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear recordatorio - Responsable inactivo (422)")
    void crear_LanzaUnprocessable_ResponsableInactivo() {
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        responsable.setEstado(EstadoLogico.INACTIVO);
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(responsable));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.crear(requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear recordatorio con frecuencia - Éxito")
    void crear_ConFrecuencia_Exito() {
        Frecuencia frecuencia = new Frecuencia();
        frecuencia.setIdFrecuencia(1);
        frecuencia.setDescripcion("Semanal los lunes");
        frecuencia.setEstado(EstadoLogico.ACTIVO);
        requestDTO.setIdFrecuencia(1);

        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(responsable));
        when(frecuenciaRepository.findById(1)).thenReturn(Optional.of(frecuencia));
        when(recordatorioIdGenerator.generarIdNeg()).thenReturn("REC-2026-000001");
        when(recordatorioRepository.save(any(Recordatorio.class))).thenReturn(recordatorio);
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);

        RecordatorioResponseDTO resultado = recordatorioService.crear(requestDTO);

        assertNotNull(resultado);
        verify(frecuenciaRepository).findById(1);
        verify(recordatorioRepository).save(any(Recordatorio.class));
    }

    @Test
    @DisplayName("Crear recordatorio - Frecuencia no encontrada (404)")
    void crear_LanzaNotFound_FrecuenciaNoExiste() {
        requestDTO.setIdFrecuencia(99);
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(responsable));
        when(recordatorioIdGenerator.generarIdNeg()).thenReturn("REC-2026-000001");
        when(frecuenciaRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.crear(requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Listar activos - Éxito, paginado")
    void listarActivos_Exito() {
        Pageable pageable = PageRequest.of(0, 20);
        when(recordatorioRepository.findByEstado(EstadoLogico.ACTIVO, pageable))
                .thenReturn(new PageImpl<>(List.of(recordatorio), pageable, 1));
        when(recordatorioMapper.toDtoList(List.of(recordatorio))).thenReturn(List.of(responseDTO));

        Page<RecordatorioResponseDTO> resultado = recordatorioService.listarActivos(pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Buscar por ID - Éxito")
    void buscarPorId_Exito() {
        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);

        RecordatorioResponseDTO resultado = recordatorioService.buscarPorId(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.getCodRecordatorio());
    }

    @Test
    @DisplayName("Buscar por ID - No encontrado (404)")
    void buscarPorId_LanzaNotFound() {
        when(recordatorioRepository.findByCodRecordatorioAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.buscarPorId(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Actualizar recordatorio - Éxito")
    void actualizar_Exito() {
        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(responsable));
        when(recordatorioRepository.save(any(Recordatorio.class))).thenReturn(recordatorio);
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);

        RecordatorioResponseDTO resultado = recordatorioService.actualizar(1, requestDTO);

        assertNotNull(resultado);
        verify(recordatorioRepository).save(recordatorio);
    }

    @Test
    @DisplayName("Actualizar recordatorio - Reasignar a otra instancia lanza 409 (B11)")
    void actualizar_LanzaConflict_CambioDeInstancia() {
        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));

        requestDTO.setCodInstancia(2);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.actualizar(1, requestDTO));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar recordatorio - No encontrado (404)")
    void actualizar_LanzaNotFound() {
        when(recordatorioRepository.findByCodRecordatorioAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.actualizar(99, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar recordatorio - Instancia no encontrada (404)")
    void actualizar_LanzaNotFound_InstanciaNoExiste() {
        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(instanciaRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.actualizar(1, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar recordatorio - Instancia inactiva (422)")
    void actualizar_LanzaUnprocessable_InstanciaInactiva() {
        when(instancia.getEstado()).thenReturn(EstadoLogico.INACTIVO);
        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.actualizar(1, requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar recordatorio - Categoría no encontrada (404)")
    void actualizar_LanzaNotFound_CategoriaNoExiste() {
        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.actualizar(1, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar recordatorio - Responsable no encontrado (404)")
    void actualizar_LanzaNotFound_ResponsableNoExiste() {
        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.actualizar(1, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar recordatorio - Frecuencia no encontrada (404)")
    void actualizar_LanzaNotFound_FrecuenciaNoExiste() {
        requestDTO.setIdFrecuencia(99);
        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(responsable));
        when(frecuenciaRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.actualizar(1, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar recordatorio - Limpiar frecuencia cuando es null")
    void actualizar_LimpiaFrecuencia_CuandoIdFrecuenciaEsNull() {
        requestDTO.setIdFrecuencia(null);
        recordatorio.setFrecuencia(new Frecuencia());

        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(responsable));
        when(recordatorioRepository.save(any(Recordatorio.class))).thenReturn(recordatorio);
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);

        recordatorioService.actualizar(1, requestDTO);

        assertNull(recordatorio.getFrecuencia());
        verify(recordatorioRepository).save(recordatorio);
    }

    // ── Actualizar - RF22 (sync Calendar) / RF23 (reset notificación) ──

    @Test
    @DisplayName("Actualizar recordatorio - Resetea notificadoProximidad cuando cambia la fecha (RF23)")
    void actualizar_ReseteaNotificadoProximidad_CuandoCambiaFecha() {
        recordatorio.setNotificadoProximidad(true);
        requestDTO.setFechaHora(recordatorio.getFechaHora().plusDays(3));

        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(responsable));
        when(recordatorioRepository.save(any(Recordatorio.class))).thenReturn(recordatorio);
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);

        recordatorioService.actualizar(1, requestDTO);

        assertFalse(recordatorio.isNotificadoProximidad());
    }

    @Test
    @DisplayName("Actualizar recordatorio - No resetea notificadoProximidad cuando la fecha no cambia (RF23)")
    void actualizar_NoReseteaNotificadoProximidad_CuandoFechaNoCambia() {
        LocalDateTime mismaFecha = LocalDateTime.now().plusDays(1);
        recordatorio.setFechaHora(mismaFecha);
        recordatorio.setNotificadoProximidad(true);
        requestDTO.setFechaHora(mismaFecha);

        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(responsable));
        when(recordatorioRepository.save(any(Recordatorio.class))).thenReturn(recordatorio);
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);

        recordatorioService.actualizar(1, requestDTO);

        assertTrue(recordatorio.isNotificadoProximidad());
    }

    @Test
    @DisplayName("Actualizar recordatorio - Actualiza el evento existente cuando el responsable no cambió (RF22)")
    void actualizar_ActualizaEventoGoogle_CuandoResponsableNoCambia() {
        recordatorio.setEventIdGoogle("evento-existente-id");
        FuncionarioGcal gcal = new FuncionarioGcal(responsable, "refresh-token", "juan@gmail.com");

        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(responsable));
        when(recordatorioRepository.save(any(Recordatorio.class))).thenReturn(recordatorio);
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);
        when(funcionarioGcalRepository.findByFuncionarioIdUsuario(2)).thenReturn(Optional.of(gcal));

        recordatorioService.actualizar(1, requestDTO);

        verify(googleCalendarService).actualizarEvento(recordatorio, "evento-existente-id", "refresh-token");
        verify(googleCalendarService, never()).crearEvento(any(), any());
        verify(googleCalendarService, never()).eliminarEvento(any(), any());
    }

    @Test
    @DisplayName("Actualizar recordatorio - Cambia responsable: borra el evento viejo y crea uno nuevo (RF22)")
    void actualizar_CambiaResponsable_BorraYCreaEvento() {
        recordatorio.setEventIdGoogle("evento-viejo-id");

        Funcionario responsableNuevo = new Funcionario();
        responsableNuevo.setIdUsuario(3);
        responsableNuevo.setNombre("María");
        responsableNuevo.setApellido("López");
        responsableNuevo.setEmail("maria.lopez@utec.edu.uy");
        responsableNuevo.setEstado(EstadoLogico.ACTIVO);
        requestDTO.setIdResponsable(3);

        FuncionarioGcal gcalViejo = new FuncionarioGcal(responsable, "refresh-token-viejo", "juan@gmail.com");
        FuncionarioGcal gcalNuevo = new FuncionarioGcal(responsableNuevo, "refresh-token-nuevo", "maria@gmail.com");

        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(3)).thenReturn(Optional.of(responsableNuevo));
        when(recordatorioRepository.save(any(Recordatorio.class))).thenReturn(recordatorio);
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);
        when(funcionarioGcalRepository.findByFuncionarioIdUsuario(2)).thenReturn(Optional.of(gcalViejo));
        when(funcionarioGcalRepository.findByFuncionarioIdUsuario(3)).thenReturn(Optional.of(gcalNuevo));
        when(googleCalendarService.crearEvento(any(), eq("refresh-token-nuevo"))).thenReturn("evento-nuevo-id");

        recordatorioService.actualizar(1, requestDTO);

        verify(googleCalendarService).eliminarEvento("evento-viejo-id", "refresh-token-viejo");
        verify(googleCalendarService).crearEvento(recordatorio, "refresh-token-nuevo");
        verify(googleCalendarService, never()).actualizarEvento(any(), any(), any());
        assertEquals("evento-nuevo-id", recordatorio.getEventIdGoogle());
    }

    @Test
    @DisplayName("Actualizar recordatorio - Evento nuevo creado en Google pero falla el guardado local, no propaga")
    void actualizar_CambiaResponsable_FallaGuardadoLocalEventIdGoogle_NoPropaga() {
        recordatorio.setEventIdGoogle("evento-viejo-id");

        Funcionario responsableNuevo = new Funcionario();
        responsableNuevo.setIdUsuario(3);
        responsableNuevo.setNombre("María");
        responsableNuevo.setApellido("López");
        responsableNuevo.setEmail("maria.lopez@utec.edu.uy");
        responsableNuevo.setEstado(EstadoLogico.ACTIVO);
        requestDTO.setIdResponsable(3);

        FuncionarioGcal gcalViejo = new FuncionarioGcal(responsable, "refresh-token-viejo", "juan@gmail.com");
        FuncionarioGcal gcalNuevo = new FuncionarioGcal(responsableNuevo, "refresh-token-nuevo", "maria@gmail.com");

        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(3)).thenReturn(Optional.of(responsableNuevo));
        when(recordatorioRepository.save(any(Recordatorio.class)))
                .thenReturn(recordatorio)
                .thenThrow(new RuntimeException("DB caída"));
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);
        when(funcionarioGcalRepository.findByFuncionarioIdUsuario(2)).thenReturn(Optional.of(gcalViejo));
        when(funcionarioGcalRepository.findByFuncionarioIdUsuario(3)).thenReturn(Optional.of(gcalNuevo));
        when(googleCalendarService.crearEvento(any(), eq("refresh-token-nuevo"))).thenReturn("evento-nuevo-id");

        RecordatorioResponseDTO resultado = assertDoesNotThrow(
                () -> recordatorioService.actualizar(1, requestDTO));

        assertNotNull(resultado);
        verify(googleCalendarService).crearEvento(recordatorio, "refresh-token-nuevo");
        verify(recordatorioRepository, times(2)).save(any(Recordatorio.class));
    }

    @Test
    @DisplayName("Actualizar recordatorio - Responsable sin Google Calendar vinculado: no sincroniza")
    void actualizar_NoSincroniza_CuandoResponsableSinGcal() {
        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(responsable));
        when(recordatorioRepository.save(any(Recordatorio.class))).thenReturn(recordatorio);
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);
        // funcionarioGcalRepository sin stub → Optional.empty() por defecto

        recordatorioService.actualizar(1, requestDTO);

        verifyNoInteractions(googleCalendarService);
    }

    @Test
    @DisplayName("Desactivar - Éxito")
    void desactivar_Exito() {
        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));

        recordatorioService.desactivar(1);

        assertEquals(EstadoLogico.INACTIVO, recordatorio.getEstado());
        verify(recordatorioRepository).save(recordatorio);
    }

    @Test
    @DisplayName("Desactivar - No encontrado (404)")
    void desactivar_LanzaNotFound() {
        when(recordatorioRepository.findByCodRecordatorioAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.desactivar(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Listar por instancia - Éxito, paginado")
    void listarPorInstancia_Exito() {
        Pageable pageable = PageRequest.of(0, 20);
        when(recordatorioRepository.findByInstanciaAndEstado(1, EstadoLogico.ACTIVO, pageable))
                .thenReturn(new PageImpl<>(List.of(recordatorio), pageable, 1));
        when(recordatorioMapper.toDtoList(List.of(recordatorio))).thenReturn(List.of(responseDTO));

        Page<RecordatorioResponseDTO> resultado = recordatorioService.listarPorInstancia(1, pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Listar por responsable - Éxito, paginado")
    void listarPorResponsable_Exito() {
        Pageable pageable = PageRequest.of(0, 20);
        when(recordatorioRepository.findByResponsableAndEstado(2, EstadoLogico.ACTIVO, pageable))
                .thenReturn(new PageImpl<>(List.of(recordatorio), pageable, 1));
        when(recordatorioMapper.toDtoList(List.of(recordatorio))).thenReturn(List.of(responseDTO));

        Page<RecordatorioResponseDTO> resultado = recordatorioService.listarPorResponsable(2, pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
    }


    @Test
    @DisplayName("Reactivar recordatorio - Éxito")
    void reactivar_Exito() {
        recordatorio.setEstado(EstadoLogico.INACTIVO);
        when(recordatorioRepository.findByCodRecordatorio(1)).thenReturn(Optional.of(recordatorio));
        when(recordatorioRepository.save(any(Recordatorio.class))).thenReturn(recordatorio);
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);

        RecordatorioResponseDTO resultado = recordatorioService.reactivar(1);

        assertNotNull(resultado);
        assertEquals(EstadoLogico.ACTIVO, recordatorio.getEstado());
        verify(recordatorioRepository).save(recordatorio);
    }

    @Test
    @DisplayName("Reactivar recordatorio - No encontrado (404)")
    void reactivar_LanzaNotFound() {
        when(recordatorioRepository.findByCodRecordatorio(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.reactivar(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactivar recordatorio - Ya estaba activo (409)")
    void reactivar_LanzaConflict_CuandoYaEstaActivo() {
        recordatorio.setEstado(EstadoLogico.ACTIVO);
        when(recordatorioRepository.findByCodRecordatorio(1)).thenReturn(Optional.of(recordatorio));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.reactivar(1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactivar recordatorio - Instancia asociada inactiva (422)")
    void reactivar_LanzaUnprocessable_InstanciaInactiva() {
        recordatorio.setEstado(EstadoLogico.INACTIVO);
        when(instancia.getEstado()).thenReturn(EstadoLogico.INACTIVO);
        when(recordatorioRepository.findByCodRecordatorio(1)).thenReturn(Optional.of(recordatorio));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.reactivar(1));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactivar recordatorio - Categoría asociada inactiva (422)")
    void reactivar_LanzaUnprocessable_CategoriaInactiva() {
        recordatorio.setEstado(EstadoLogico.INACTIVO);
        categoria.setEstado(EstadoLogico.INACTIVO);
        when(recordatorioRepository.findByCodRecordatorio(1)).thenReturn(Optional.of(recordatorio));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.reactivar(1));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactivar recordatorio - Responsable asociado inactivo (422)")
    void reactivar_LanzaUnprocessable_ResponsableInactivo() {
        recordatorio.setEstado(EstadoLogico.INACTIVO);
        responsable.setEstado(EstadoLogico.INACTIVO);
        when(recordatorioRepository.findByCodRecordatorio(1)).thenReturn(Optional.of(recordatorio));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.reactivar(1));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    // ── Marcar cumplido (RF27) ───────────────────────────────

    @Test
    @DisplayName("Marcar cumplido - Éxito")
    void marcarCumplido_Exito() {
        recordatorio.setCumplido(false);
        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));
        when(recordatorioRepository.save(any(Recordatorio.class))).thenReturn(recordatorio);
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);

        RecordatorioResponseDTO resultado = recordatorioService.marcarCumplido(1);

        assertNotNull(resultado);
        assertTrue(recordatorio.isCumplido());
        verify(recordatorioRepository).save(recordatorio);
    }

    @Test
    @DisplayName("Marcar cumplido - No encontrado (404)")
    void marcarCumplido_LanzaNotFound() {
        when(recordatorioRepository.findByCodRecordatorioAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.marcarCumplido(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Marcar cumplido - Ya estaba cumplido (409)")
    void marcarCumplido_LanzaConflict_CuandoYaEstaCumplido() {
        recordatorio.setCumplido(true);
        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.marcarCumplido(1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(recordatorioRepository, never()).save(any());
    }
}