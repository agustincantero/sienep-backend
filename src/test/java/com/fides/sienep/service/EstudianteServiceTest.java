package com.fides.sienep.service;

import com.fides.sienep.dto.EstudianteRequestDTO;
import com.fides.sienep.dto.EstudianteResponseDTO;
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

    // ── ¿Por qué mockeamos PasswordEncoder? ──────────────────────────────────
    // BCryptPasswordEncoder hace cálculos costosos (por diseño, para frenar
    // ataques de fuerza bruta). En tests unitarios no queremos ni necesitamos
    // ese costo — lo reemplazamos por un mock que devuelve un string fijo.
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EstudianteService estudianteService;

    private Estudiante estudianteExistente;

    @BeforeEach
    void setUp() {
        estudianteExistente = new Estudiante();
        estudianteExistente.setNombre("Ana");
        estudianteExistente.setApellido("García");
        // CI uruguaya real válida (dígito verificador correcto)
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

        when(estudianteRepository.findById(1))
                .thenReturn(Optional.of(estudianteExistente));

        EstudianteResponseDTO resultado = estudianteService.obtenerPorId(1);

        assertNotNull(resultado);
        assertEquals("Ana", resultado.getNombre());
        assertEquals("47520640", resultado.getDocumento());
        verify(estudianteRepository, times(1)).findById(1);
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
        // Nunca debe intentar guardar
        verify(estudianteRepository, never()).save(any());
    }

    // =========================================================
    // TEST 4 — crear: CI uruguaya inválida → 400
    // =========================================================

    /**
     * "47520641" es la CI "47520640" con el dígito verificador cambiado
     * de 0 a 1 → el algoritmo de validación debe rechazarla.
     *
     * ¿Por qué testear esto específicamente?
     * El validador recibe el string del DTO — si el service no lo llama,
     * el test falla aunque el código de la validación sea correcto.
     * Este test verifica que el SERVICE efectivamente invoca al validador.
     */
    @Test
    void crear_DebeLanzar400_CuandoCIUruguayaEsInvalida() {

        EstudianteRequestDTO dto = buildDtoValido();
        dto.setDocumento("47520641"); // dígito verificador incorrecto

        when(estudianteRepository.existsByDocumento("47520641")).thenReturn(false);
        when(estudianteRepository.existsByEmail(dto.getEmail())).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.crear(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("cédula"));
        verify(estudianteRepository, never()).save(any());
    }

    // =========================================================
    // TEST 5 — crear: menor de edad → 400
    // =========================================================

    /**
     * Usamos un documento extranjero (ARG) para evitar que falle
     * por la validación de CI uruguaya antes de llegar a la de edad.
     * Cada test debe probar UNA sola cosa — esto se llama "un assert
     * conceptual por test".
     */
    @Test
    void crear_DebeLanzar400_CuandoEstudianteEsMenorDeEdad() {

        EstudianteRequestDTO dto = buildDtoValido();
        dto.setPaisDocumento(PaisDocumento.ARG);
        // Fecha que hace al estudiante menor de 18
        dto.setFechaNacimiento(LocalDate.now().minusYears(17));

        when(estudianteRepository.existsByDocumento(anyString())).thenReturn(false);
        when(estudianteRepository.existsByEmail(anyString())).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.crear(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("mayor de edad"));
        verify(estudianteRepository, never()).save(any());
    }

    // =========================================================
    // TEST 6 — darDeBaja: baja lógica → estado INACTIVO
    // =========================================================

    /**
     * Verifica que darDeBaja cambia el estado a INACTIVO y llama a save().
     * NO elimina el registro físicamente (baja lógica).
     *
     * Este test documenta el contrato de que NUNCA hay deleteById.
     */
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
        // Garantía explícita: nunca se eliminó físicamente
        verify(estudianteRepository, never()).deleteById(any());
    }

    // =========================================================
    // HELPER — construye un DTO válido reutilizable
    // =========================================================

    /**
     * Centralizar la construcción del DTO base en un método privado
     * sigue el principio DRY (Don't Repeat Yourself).
     * Cada test solo sobreescribe el campo que quiere probar.
     */
    private EstudianteRequestDTO buildDtoValido() {
        EstudianteRequestDTO dto = new EstudianteRequestDTO();
        dto.setNombre("Carlos");
        dto.setApellido("Pérez");
        dto.setDocumento("47520640");      // CI uruguaya válida
        dto.setPaisDocumento(PaisDocumento.URY);
        dto.setEmail("carlos.perez@utec.edu.uy");
        dto.setContrasenia("Temp1234!");
        dto.setFechaNacimiento(LocalDate.of(1998, 7, 20)); // mayor de edad
        return dto;
    }
}