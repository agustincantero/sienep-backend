package com.fides.sienep.service;

import com.fides.sienep.dto.InvolucradoRequestDTO;
import com.fides.sienep.dto.InvolucradoResponseDTO;
import com.fides.sienep.mapper.InvolucradoMapper;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.IncidenciaRepository;
import com.fides.sienep.repository.InvolucradoRepository;
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
class InvolucradoServiceTest {

    @Mock private InvolucradoRepository involucradoRepository;
    @Mock private IncidenciaRepository incidenciaRepository;
    @Mock private InvolucradoMapper involucradoMapper;

    @InjectMocks
    private InvolucradoService involucradoService;

    private Incidencia incidencia;
    private Involucrado involucrado;
    private InvolucradoRequestDTO requestDTO;
    private InvolucradoResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        incidencia = new Incidencia();
        incidencia.setCodInstancia(1);
        incidencia.setEstado(EstadoLogico.ACTIVO);

        Involucrado.InvolucradoId id = new Involucrado.InvolucradoId(1, "Juan Pérez");

        involucrado = new Involucrado();
        involucrado.setId(id);
        involucrado.setInstancia(incidencia);
        involucrado.setEstado(EstadoLogico.ACTIVO);

        requestDTO = new InvolucradoRequestDTO();
        requestDTO.setNomInvolucrado("Juan Pérez");

        responseDTO = InvolucradoResponseDTO.builder()
                .codInstancia(1)
                .nomInvolucrado("Juan Pérez")
                .estado(EstadoLogico.ACTIVO)
                .build();
    }

    @Test
    @DisplayName("Agregar involucrado - Éxito")
    void agregar_Exito() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));
        when(involucradoRepository.findByIdCodInstanciaAndIdNomInvolucrado(1, "Juan Pérez"))
                .thenReturn(Optional.empty());
        when(incidenciaRepository.findById(1)).thenReturn(Optional.of(incidencia));
        when(involucradoRepository.save(any(Involucrado.class))).thenReturn(involucrado);
        when(involucradoMapper.toDto(involucrado)).thenReturn(responseDTO);

        InvolucradoResponseDTO resultado = involucradoService.agregar(1, requestDTO);

        assertNotNull(resultado);
        assertEquals("Juan Pérez", resultado.getNomInvolucrado());
        verify(involucradoRepository).save(any(Involucrado.class));
    }

    @Test
    @DisplayName("Agregar involucrado - Incidencia no encontrada (404)")
    void agregar_LanzaNotFound_IncidenciaNoExiste() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> involucradoService.agregar(1, requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(involucradoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Agregar involucrado - Ya existe activo (409)")
    void agregar_LanzaConflict_InvolucradoYaExiste() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));
        when(involucradoRepository.findByIdCodInstanciaAndIdNomInvolucrado(1, "Juan Pérez"))
                .thenReturn(Optional.of(involucrado));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> involucradoService.agregar(1, requestDTO));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(involucradoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Agregar involucrado - Reactivar inactivo")
    void agregar_Reactiva_InvolucradoInactivo() {
        involucrado.setEstado(EstadoLogico.INACTIVO);
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));
        when(involucradoRepository.findByIdCodInstanciaAndIdNomInvolucrado(1, "Juan Pérez"))
                .thenReturn(Optional.of(involucrado));
        when(involucradoRepository.save(involucrado)).thenReturn(involucrado);
        when(involucradoMapper.toDto(involucrado)).thenReturn(responseDTO);

        InvolucradoResponseDTO resultado = involucradoService.agregar(1, requestDTO);

        assertNotNull(resultado);
        assertEquals(EstadoLogico.ACTIVO, involucrado.getEstado());
        verify(involucradoRepository).save(involucrado);
    }

    @Test
    @DisplayName("Listar involucrados - Éxito")
    void listar_Exito() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));
        when(involucradoRepository.findByIdCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(List.of(involucrado));
        when(involucradoMapper.toDtoList(List.of(involucrado))).thenReturn(List.of(responseDTO));

        List<InvolucradoResponseDTO> resultado = involucradoService.listar(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Listar involucrados - Incidencia no encontrada (404)")
    void listar_LanzaNotFound_IncidenciaNoExiste() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> involucradoService.listar(1));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Desactivar involucrado - Éxito")
    void desactivar_Exito() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));
        when(involucradoRepository.findByIdCodInstanciaAndIdNomInvolucrado(1, "Juan Pérez"))
                .thenReturn(Optional.of(involucrado));

        involucradoService.desactivar(1, "Juan Pérez");

        assertEquals(EstadoLogico.INACTIVO, involucrado.getEstado());
        verify(involucradoRepository).save(involucrado);
    }

    @Test
    @DisplayName("Desactivar involucrado - No encontrado (404)")
    void desactivar_LanzaNotFound_InvolucradoNoExiste() {
        when(incidenciaRepository.findByCodInstanciaAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(incidencia));
        when(involucradoRepository.findByIdCodInstanciaAndIdNomInvolucrado(1, "No Existe"))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> involucradoService.desactivar(1, "No Existe"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(involucradoRepository, never()).save(any());
    }
}