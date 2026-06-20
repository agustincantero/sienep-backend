package com.fides.sienep.service;

import com.fides.sienep.dto.EstudianteRequestDTO;
import com.fides.sienep.dto.EstudianteResponseDTO;
import com.fides.sienep.mapper.EstudianteMapper;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.GrupoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceTest {

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    // ── ¿Por qué mockeamos EstudianteMapper? ─────────────────────────────────
    // EstudianteService ahora delega la conversión entidad→DTO al mapper.
    // En tests unitarios no queremos ejecutar la implementación real del mapper
    // (eso sería un test de integración). Lo mockeamos para controlar
    // exactamente qué devuelve y mantener el test enfocado en la lógica
    // del service, no en la del mapper.
    @Mock
    private EstudianteMapper estudianteMapper;

    @InjectMocks
    private EstudianteService estudianteService;

    private Estudiante estudianteExistente;

    @BeforeEach
    void setUp() {
        estudianteExistente = new Estudiante();
        estudianteExistente.setNombre("Ana");
        estudianteExistente.setApellido("García");
        estudianteExistente.setDocumento("47520640");
        estudianteExistente.setPaisDocumento(PaisDocumento.URY);
        estudianteExistente.setEmail("ana.garcia@utec.edu.uy");
        estudianteExistente.setFechaNacimiento(LocalDate.of(2000, 3, 15));
        estudianteExistente.setEstado(EstadoLogico.ACTIVO);
        estudianteExistente.setTelefonos(new ArrayList<>());
        estudianteExistente.setGrupos(new ArrayList<>());
    }

    // =========================================================
    // TEST 1 — obtenerPorId: camino feliz
    // =========================================================

    @Test
    void obtenerPorId_DebeRetornarDTO_CuandoElEstudianteExiste() {

        // El mapper mock devuelve un DTO con los datos que nos interesan verificar
        EstudianteResponseDTO dtoEsperado = EstudianteResponseDTO.builder()
                .nombre("Ana")
                .documento("47520640")
                .build();

        when(estudianteRepository.findById(1))
                .thenReturn(Optional.of(estudianteExistente));
        when(estudianteMapper.toDto(estudianteExistente))
                .thenReturn(dtoEsperado);

        EstudianteResponseDTO resultado = estudianteService.obtenerPorId(1);

        assertNotNull(resultado);
        assertEquals("Ana", resultado.getNombre());
        assertEquals("47520640", resultado.getDocumento());
        verify(estudianteRepository, times(1)).findById(1);
        verify(estudianteMapper, times(1)).toDto(estudianteExistente);
    }

    // =========================================================
    // TEST 2 — obtenerPorId: estudiante no existe → 404
    // =========================================================

    @Test
    void obtenerPorId_DebeLanzar404_CuandoNoExiste() {

        when(estudianteRepository.findById(99))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.obtenerPorId(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Estudiante no encontrado"));
        verify(estudianteRepository, times(1)).findById(99);
        // El mapper nunca debe ser llamado si el estudiante no existe
        verify(estudianteMapper, never()).toDto(any());
    }

    // =========================================================
    // TEST 3 — crear: documento duplicado → 409
    // =========================================================

    @Test
    void crear_DebeLanzar409_CuandoDocumentoYaExiste() {

        EstudianteRequestDTO dto = buildDtoValido();
        when(estudianteRepository.existsByDocumento("47520640")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.crear(dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("documento"));
        verify(estudianteRepository, never()).save(any());
        verify(estudianteMapper, never()).toDto(any());
    }

    // =========================================================
    // TEST 4 — crear: CI uruguaya inválida → 400
    // =========================================================

    @Test
    void crear_DebeLanzar400_CuandoCIUruguayaEsInvalida() {

        EstudianteRequestDTO dto = buildDtoValido();
        dto.setDocumento("47520642");

        when(estudianteRepository.existsByDocumento("47520642")).thenReturn(false);
        when(estudianteRepository.existsByEmail(dto.getEmail())).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.crear(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("cédula"));
        verify(estudianteRepository, never()).save(any());
        verify(estudianteMapper, never()).toDto(any());
    }

    // =========================================================
    // TEST 5 — crear: menor de edad → 400
    // =========================================================

    @Test
    void crear_DebeLanzar400_CuandoEstudianteEsMenorDeEdad() {

        EstudianteRequestDTO dto = buildDtoValido();
        dto.setPaisDocumento(PaisDocumento.ARG);
        dto.setFechaNacimiento(LocalDate.now().minusYears(17));

        when(estudianteRepository.existsByDocumento(anyString())).thenReturn(false);
        when(estudianteRepository.existsByEmail(anyString())).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.crear(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("mayor de edad"));
        verify(estudianteRepository, never()).save(any());
        verify(estudianteMapper, never()).toDto(any());
    }

    // =========================================================
    // TEST 6 — darDeBaja: baja lógica → estado INACTIVO
    // =========================================================

    @Test
    void darDeBaja_DebePonerEstadoInactivo_YGuardar() {

        when(estudianteRepository.findById(1))
                .thenReturn(Optional.of(estudianteExistente));
        when(estudianteRepository.save(any(Estudiante.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        estudianteService.darDeBaja(1);

        assertEquals(EstadoLogico.INACTIVO, estudianteExistente.getEstado(),
                "El estado debe ser INACTIVO después de dar de baja");
        verify(estudianteRepository, times(1)).save(estudianteExistente);
        verify(estudianteRepository, never()).deleteById(any());
        // darDeBaja no devuelve DTO — el mapper nunca debe ser llamado
        verify(estudianteMapper, never()).toDto(any());
    }

    // =========================================================
    // HELPER
    // =========================================================

    private EstudianteRequestDTO buildDtoValido() {
        EstudianteRequestDTO dto = new EstudianteRequestDTO();
        dto.setNombre("Carlos");
        dto.setApellido("Pérez");
        dto.setDocumento("47520640");
        dto.setPaisDocumento(PaisDocumento.URY);
        dto.setEmail("carlos.perez@utec.edu.uy");
        dto.setContrasenia("Temp1234!");
        dto.setFechaNacimiento(LocalDate.of(1998, 7, 20));
        return dto;
    }
}