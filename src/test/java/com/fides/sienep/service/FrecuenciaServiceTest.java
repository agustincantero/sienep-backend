package com.fides.sienep.service;

import com.fides.sienep.dto.FrecuenciaRequestDTO;
import com.fides.sienep.dto.FrecuenciaResponseDTO;
import com.fides.sienep.mapper.FrecuenciaMapper;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.FrecuenciaRepository;
import com.fides.sienep.repository.RecordatorioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FrecuenciaServiceTest {

    @Mock private FrecuenciaRepository frecuenciaRepository;
    @Mock private RecordatorioRepository recordatorioRepository;
    @Mock private FrecuenciaMapper frecuenciaMapper;

    @InjectMocks
    private FrecuenciaService frecuenciaService;

    private Frecuencia frecuencia;
    private FrecuenciaResponseDTO responseDTO;
    private FrecuenciaRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        frecuencia = new Frecuencia();
        frecuencia.setIdFrecuencia(1);
        frecuencia.setDescripcion("Diaria");
        frecuencia.setTipo(TipoFrecuencia.DIARIA);
        frecuencia.setEstado(EstadoLogico.ACTIVO);

        requestDTO = new FrecuenciaRequestDTO();
        requestDTO.setDescripcion("Diaria");
        requestDTO.setTipo(TipoFrecuencia.DIARIA);

        responseDTO = FrecuenciaResponseDTO.builder()
                .idFrecuencia(1)
                .descripcion("Diaria")
                .tipo(TipoFrecuencia.DIARIA)
                .estado(EstadoLogico.ACTIVO)
                .build();
    }

    @Test
    @DisplayName("Crear frecuencia diaria - Éxito")
    void crear_Diaria_Exito() {
        when(frecuenciaRepository.save(any(Frecuencia.class))).thenReturn(frecuencia);
        when(frecuenciaMapper.toDto(frecuencia)).thenReturn(responseDTO);

        FrecuenciaResponseDTO resultado = frecuenciaService.crear(requestDTO);

        assertNotNull(resultado);
        assertEquals(1, resultado.getIdFrecuencia());
        verify(frecuenciaRepository).save(any(Frecuencia.class));
    }

    @Test
    @DisplayName("Crear frecuencia diaria con intervalo - Error (400)")
    void crear_Diaria_ConIntervalo_LanzaBadRequest() {
        requestDTO.setIntDias(5);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> frecuenciaService.crear(requestDTO));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(frecuenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear frecuencia semanal sin día - Error (400)")
    void crear_Semanal_SinDia_LanzaBadRequest() {
        requestDTO.setTipo(TipoFrecuencia.SEMANAL);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> frecuenciaService.crear(requestDTO));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(frecuenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear frecuencia semanal con día - Éxito")
    void crear_Semanal_ConDia_Exito() {
        requestDTO.setTipo(TipoFrecuencia.SEMANAL);
        requestDTO.setDiaSemana(DiaSemana.LUNES);
        frecuencia.setTipo(TipoFrecuencia.SEMANAL);
        frecuencia.setDiaSemana(DiaSemana.LUNES);

        when(frecuenciaRepository.save(any(Frecuencia.class))).thenReturn(frecuencia);
        when(frecuenciaMapper.toDto(frecuencia)).thenReturn(responseDTO);

        FrecuenciaResponseDTO resultado = frecuenciaService.crear(requestDTO);

        assertNotNull(resultado);
        verify(frecuenciaRepository).save(any(Frecuencia.class));
    }

    @Test
    @DisplayName("Crear frecuencia mensual sin campos extra - Éxito (M7: mensual no usa intDias ni diaSemana)")
    void crear_Mensual_Exito() {
        requestDTO.setTipo(TipoFrecuencia.MENSUAL);
        frecuencia.setTipo(TipoFrecuencia.MENSUAL);

        when(frecuenciaRepository.save(any(Frecuencia.class))).thenReturn(frecuencia);
        when(frecuenciaMapper.toDto(frecuencia)).thenReturn(responseDTO);

        FrecuenciaResponseDTO resultado = frecuenciaService.crear(requestDTO);

        assertNotNull(resultado);
        verify(frecuenciaRepository).save(any(Frecuencia.class));
    }

    @Test
    @DisplayName("Crear frecuencia mensual con intervalo de días - Error (400, M7: el campo no se usa)")
    void crear_Mensual_ConIntDias_LanzaBadRequest() {
        requestDTO.setTipo(TipoFrecuencia.MENSUAL);
        requestDTO.setIntDias(15);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> frecuenciaService.crear(requestDTO));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(frecuenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear frecuencia mensual con día de la semana - Error (400, M7: el campo no se usa)")
    void crear_Mensual_ConDiaSemana_LanzaBadRequest() {
        requestDTO.setTipo(TipoFrecuencia.MENSUAL);
        requestDTO.setDiaSemana(DiaSemana.LUNES);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> frecuenciaService.crear(requestDTO));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(frecuenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear frecuencia personalizada sin intervalo - Error (400, M7: el campo que sí se usa)")
    void crear_Personalizada_SinIntervalo_LanzaBadRequest() {
        requestDTO.setTipo(TipoFrecuencia.PERSONALIZADA);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> frecuenciaService.crear(requestDTO));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(frecuenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear frecuencia personalizada con intervalo - Éxito")
    void crear_Personalizada_ConIntervalo_Exito() {
        requestDTO.setTipo(TipoFrecuencia.PERSONALIZADA);
        requestDTO.setIntDias(15);
        frecuencia.setTipo(TipoFrecuencia.PERSONALIZADA);
        frecuencia.setIntDias(15);

        when(frecuenciaRepository.save(any(Frecuencia.class))).thenReturn(frecuencia);
        when(frecuenciaMapper.toDto(frecuencia)).thenReturn(responseDTO);

        FrecuenciaResponseDTO resultado = frecuenciaService.crear(requestDTO);

        assertNotNull(resultado);
        verify(frecuenciaRepository).save(any(Frecuencia.class));
    }

    @Test
    @DisplayName("Listar activas - Éxito")
    void listarActivas_Exito() {
        when(frecuenciaRepository.findByEstado(EstadoLogico.ACTIVO)).thenReturn(List.of(frecuencia));
        when(frecuenciaMapper.toDtoList(List.of(frecuencia))).thenReturn(List.of(responseDTO));

        List<FrecuenciaResponseDTO> resultado = frecuenciaService.listarActivas();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Buscar por ID - Éxito")
    void buscarPorId_Exito() {
        when(frecuenciaRepository.findByIdFrecuenciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(frecuencia));
        when(frecuenciaMapper.toDto(frecuencia)).thenReturn(responseDTO);

        FrecuenciaResponseDTO resultado = frecuenciaService.buscarPorId(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.getIdFrecuencia());
    }

    @Test
    @DisplayName("Buscar por ID - No encontrada (404)")
    void buscarPorId_LanzaNotFound() {
        when(frecuenciaRepository.findByIdFrecuenciaAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> frecuenciaService.buscarPorId(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Actualizar frecuencia - Éxito")
    void actualizar_Exito() {
        when(frecuenciaRepository.findByIdFrecuenciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(frecuencia));
        when(frecuenciaRepository.save(any(Frecuencia.class))).thenReturn(frecuencia);
        when(frecuenciaMapper.toDto(frecuencia)).thenReturn(responseDTO);

        FrecuenciaResponseDTO resultado = frecuenciaService.actualizar(1, requestDTO);

        assertNotNull(resultado);
        verify(frecuenciaRepository).save(frecuencia);
    }

    @Test
    @DisplayName("Actualizar frecuencia - No encontrada (404)")
    void actualizar_LanzaNotFound() {
        when(frecuenciaRepository.findByIdFrecuenciaAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> frecuenciaService.actualizar(99, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(frecuenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dar de baja - Éxito")
    void darDeBaja_Exito() {
        when(frecuenciaRepository.findByIdFrecuenciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(frecuencia));
        when(recordatorioRepository.existsByFrecuencia_IdFrecuenciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(false);

        frecuenciaService.darDeBaja(1);

        assertEquals(EstadoLogico.INACTIVO, frecuencia.getEstado());
        verify(frecuenciaRepository).save(frecuencia);
    }

    @Test
    @DisplayName("Dar de baja - No encontrada (404)")
    void darDeBaja_LanzaNotFound() {
        when(frecuenciaRepository.findByIdFrecuenciaAndEstado(99, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> frecuenciaService.darDeBaja(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Dar de baja - En uso por recordatorios activos (409, M8)")
    void darDeBaja_LanzaConflict_EnUsoPorRecordatorios() {
        when(frecuenciaRepository.findByIdFrecuenciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(frecuencia));
        when(recordatorioRepository.existsByFrecuencia_IdFrecuenciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> frecuenciaService.darDeBaja(1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(EstadoLogico.ACTIVO, frecuencia.getEstado());
        verify(frecuenciaRepository, never()).save(any());
    }

    // =========================================================
    // REACTIVAR (M8)
    // =========================================================

    @Test
    @DisplayName("Reactivar - Estado pasa a ACTIVO")
    void reactivar_DebePonerEstadoActivo() {
        frecuencia.setEstado(EstadoLogico.INACTIVO);
        when(frecuenciaRepository.findById(1)).thenReturn(Optional.of(frecuencia));
        when(frecuenciaRepository.save(any(Frecuencia.class))).thenReturn(frecuencia);
        when(frecuenciaMapper.toDto(frecuencia)).thenReturn(responseDTO);

        FrecuenciaResponseDTO resultado = frecuenciaService.reactivar(1);

        assertNotNull(resultado);
        assertEquals(EstadoLogico.ACTIVO, frecuencia.getEstado());
        verify(frecuenciaRepository).save(frecuencia);
    }

    @Test
    @DisplayName("Reactivar - No encontrada (404)")
    void reactivar_LanzaNotFound() {
        when(frecuenciaRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> frecuenciaService.reactivar(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(frecuenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactivar - Ya está activa (409)")
    void reactivar_LanzaConflict_YaActiva() {
        when(frecuenciaRepository.findById(1)).thenReturn(Optional.of(frecuencia));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> frecuenciaService.reactivar(1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(frecuenciaRepository, never()).save(any());
    }
}