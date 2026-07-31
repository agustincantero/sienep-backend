package com.fides.sienep.service;

import com.fides.sienep.dto.TelefonoRequestDTO;
import com.fides.sienep.dto.TelefonoResponseDTO;
import com.fides.sienep.mapper.TelefonoMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Telefono;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.TelefonoRepository;
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
class TelefonoServiceTest {

    @Mock private TelefonoRepository telefonoRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private TelefonoMapper telefonoMapper;

    @InjectMocks
    private TelefonoService telefonoService;

    private Estudiante estudiante;
    private Telefono telefono;
    private TelefonoRequestDTO requestDTO;
    private TelefonoResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        estudiante = new Estudiante();
        estudiante.setIdUsuario(1);
        estudiante.setNombre("Ana");
        estudiante.setEstado(EstadoLogico.ACTIVO);

        telefono = new Telefono(estudiante, "099123456");

        requestDTO = new TelefonoRequestDTO();
        requestDTO.setIdUsuario(1);
        requestDTO.setNumero("099123456");

        responseDTO = new TelefonoResponseDTO();
        responseDTO.setIdUsuario(1);
        responseDTO.setNumero("099123456");
    }

    // ── Listar por estudiante ─────────────────────────────────

    @Test
    @DisplayName("Listar por estudiante - Éxito")
    void listarPorEstudiante_Exito() {
        when(telefonoRepository.findByIdIdUsuarioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(List.of(telefono));
        when(telefonoMapper.toDtoList(List.of(telefono))).thenReturn(List.of(responseDTO));

        List<TelefonoResponseDTO> resultado = telefonoService.listarPorEstudiante(1);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Listar por estudiante - Sin teléfonos, lista vacía")
    void listarPorEstudiante_SinTelefonos_ListaVacia() {
        when(telefonoRepository.findByIdIdUsuarioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(List.of());
        when(telefonoMapper.toDtoList(List.of())).thenReturn(List.of());

        List<TelefonoResponseDTO> resultado = telefonoService.listarPorEstudiante(1);

        assertTrue(resultado.isEmpty());
    }

    // ── Agregar ───────────────────────────────────────────────

    @Test
    @DisplayName("Agregar teléfono - Éxito")
    void agregar_Exito() {
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(telefonoRepository.existsById(any(Telefono.Id.class))).thenReturn(false);
        when(telefonoMapper.toDto(any(Telefono.class))).thenReturn(responseDTO);

        TelefonoResponseDTO resultado = telefonoService.agregar(requestDTO);

        assertNotNull(resultado);
        assertEquals("099123456", resultado.getNumero());
        verify(telefonoRepository).save(any(Telefono.class));
    }

    @Test
    @DisplayName("Agregar teléfono - Estudiante no encontrado (404)")
    void agregar_LanzaNotFound_EstudianteNoExiste() {
        when(estudianteRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> telefonoService.agregar(requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(telefonoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Agregar teléfono - Estudiante inactivo (422, B7)")
    void agregar_LanzaUnprocessable_EstudianteInactivo() {
        estudiante.setEstado(EstadoLogico.INACTIVO);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> telefonoService.agregar(requestDTO));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(telefonoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Agregar teléfono - Ya existe para el estudiante (409)")
    void agregar_LanzaConflict_TelefonoYaExiste() {
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(telefonoRepository.existsById(any(Telefono.Id.class))).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> telefonoService.agregar(requestDTO));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(telefonoRepository, never()).save(any());
    }

    // ── Eliminar ──────────────────────────────────────────────

    @Test
    @DisplayName("Eliminar teléfono - Éxito, marca ELIMINADO")
    void eliminar_Exito() {
        Telefono.Id id = new Telefono.Id(1, "099123456");
        when(telefonoRepository.findById(id)).thenReturn(Optional.of(telefono));

        telefonoService.eliminar(1, "099123456");

        // B3 — save() explícito por consistencia con el resto de los services,
        // aunque el dirty-checking de Hibernate ya hubiera persistido el cambio.
        assertEquals(EstadoLogico.ELIMINADO, telefono.getEstado());
        verify(telefonoRepository).save(telefono);
    }

    @Test
    @DisplayName("Eliminar teléfono - No encontrado (404)")
    void eliminar_LanzaNotFound() {
        Telefono.Id id = new Telefono.Id(1, "000000000");
        when(telefonoRepository.findById(id)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> telefonoService.eliminar(1, "000000000"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}