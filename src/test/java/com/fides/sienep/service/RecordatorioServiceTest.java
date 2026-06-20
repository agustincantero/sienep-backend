package com.fides.sienep.service;

import com.fides.sienep.dto.RecordatorioRequestDTO;
import com.fides.sienep.dto.RecordatorioResponseDTO;
import com.fides.sienep.mapper.RecordatorioMapper;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.*;
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
    @DisplayName("Crear recordatorio con frecuencia - Éxito")
    void crear_ConFrecuencia_Exito() {
        Frecuencia frecuencia = new Frecuencia();
        frecuencia.setIdFrecuencia(1);
        frecuencia.setDescripcion("Semanal los lunes");
        requestDTO.setIdFrecuencia(1);

        when(instanciaRepository.findById(1)).thenReturn(Optional.of(instancia));
        when(categoriaRecordatorioRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(funcionarioRepository.findById(2)).thenReturn(Optional.of(responsable));
        when(frecuenciaRepository.findById(1)).thenReturn(Optional.of(frecuencia));
        when(recordatorioRepository.save(any(Recordatorio.class))).thenReturn(recordatorio);
        when(recordatorioMapper.toDto(recordatorio)).thenReturn(responseDTO);

        RecordatorioResponseDTO resultado = recordatorioService.crear(requestDTO);

        assertNotNull(resultado);
        verify(frecuenciaRepository).findById(1);
        verify(recordatorioRepository).save(any(Recordatorio.class));
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
}