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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
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

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
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

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
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
    @DisplayName("Listar activos - Éxito")
    void listarActivos_Exito() {
        when(recordatorioRepository.findByEstado(EstadoLogico.ACTIVO)).thenReturn(List.of(recordatorio));
        when(recordatorioMapper.toDtoList(List.of(recordatorio))).thenReturn(List.of(responseDTO));

        List<RecordatorioResponseDTO> resultado = recordatorioService.listarActivos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
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

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
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

    @Test
    @DisplayName("Dar de baja - Éxito")
    void darDeBaja_Exito() {
        when(recordatorioRepository.findByCodRecordatorioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(recordatorio));

        recordatorioService.darDeBaja(1);

        assertEquals(EstadoLogico.INACTIVO, recordatorio.getEstado());
        verify(recordatorioRepository).save(recordatorio);
    }

    @Test
    @DisplayName("Dar de baja - No encontrado (404)")
    void darDeBaja_LanzaNotFound() {
        when(recordatorioRepository.findByCodRecordatorioAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> recordatorioService.darDeBaja(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Listar por instancia - Éxito")
    void listarPorInstancia_Exito() {
        when(recordatorioRepository.findByInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(List.of(recordatorio));
        when(recordatorioMapper.toDtoList(List.of(recordatorio))).thenReturn(List.of(responseDTO));

        List<RecordatorioResponseDTO> resultado = recordatorioService.listarPorInstancia(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Listar por responsable - Éxito")
    void listarPorResponsable_Exito() {
        when(recordatorioRepository.findByResponsableAndEstado(2, EstadoLogico.ACTIVO))
                .thenReturn(List.of(recordatorio));
        when(recordatorioMapper.toDtoList(List.of(recordatorio))).thenReturn(List.of(responseDTO));

        List<RecordatorioResponseDTO> resultado = recordatorioService.listarPorResponsable(2);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }
}