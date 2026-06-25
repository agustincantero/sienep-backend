package com.fides.sienep.service;

import com.fides.sienep.dto.InformeAdjuntoRequestDTO;
import com.fides.sienep.dto.InformeAdjuntoResponseDTO;
import com.fides.sienep.mapper.InformeAdjuntoMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.InformeAdjunto;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.InformeAdjuntoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InformeAdjuntoServiceTest {

    @Mock private InformeAdjuntoRepository informeRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private InformeAdjuntoMapper informeMapper;

    @InjectMocks
    private InformeAdjuntoService informeService;

    private Estudiante estudianteMock;
    private InformeAdjunto informeMock;
    private InformeAdjuntoResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        estudianteMock = new Estudiante();
        estudianteMock.setNombre("Pedro");
        estudianteMock.setApellido("Díaz");
        estudianteMock.setDocumento("12345679");
        estudianteMock.setPaisDocumento(PaisDocumento.ARG);
        estudianteMock.setEmail("pedro.diaz@utec.edu.uy");
        estudianteMock.setFechaNacimiento(LocalDate.of(2000, 1, 1));
        estudianteMock.setEstado(EstadoLogico.ACTIVO);
        estudianteMock.setTelefonos(new ArrayList<>());
        estudianteMock.setGrupos(new ArrayList<>());

        informeMock = InformeAdjunto.builder()
                .idInforme(1)
                .estudiante(estudianteMock)
                .urlArchivo("https://storage.example.com/informe.pdf")
                .nombre("informe_2024")
                .estado(EstadoLogico.ACTIVO)
                .build();

        responseDTO = InformeAdjuntoResponseDTO.builder()
                .idInforme(1)
                .idUsuario(1)
                .urlArchivo("https://storage.example.com/informe.pdf")
                .nombre("informe_2024")
                .estado(EstadoLogico.ACTIVO)
                .build();
    }

    // ── Adjuntar ──────────────────────────────────────────────

    @Test
    @DisplayName("Adjuntar informe - Éxito con PDF")
    void adjuntar_Exito() {
        InformeAdjuntoRequestDTO dto = new InformeAdjuntoRequestDTO();
        dto.setIdUsuario(1);
        dto.setUrlArchivo("https://storage.example.com/informe.pdf");
        dto.setNombre("informe_2024");

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));
        when(informeRepository.existsByEstudianteIdUsuarioAndNombre(1, "informe_2024")).thenReturn(false);
        when(informeRepository.save(any(InformeAdjunto.class))).thenReturn(informeMock);
        when(informeMapper.toDto(any(InformeAdjunto.class))).thenReturn(responseDTO);

        InformeAdjuntoResponseDTO result = informeService.adjuntar(dto);

        assertNotNull(result);
        assertEquals("informe_2024", result.getNombre());
        verify(informeRepository).save(any(InformeAdjunto.class));
    }

    @Test
    @DisplayName("Adjuntar informe - Éxito con extensión JPG en mayúsculas")
    void adjuntar_Exito_ExtensionMayuscula() {
        InformeAdjuntoRequestDTO dto = new InformeAdjuntoRequestDTO();
        dto.setIdUsuario(1);
        dto.setUrlArchivo("https://storage.example.com/imagen.JPG");
        dto.setNombre("imagen_2024");

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));
        when(informeRepository.existsByEstudianteIdUsuarioAndNombre(1, "imagen_2024")).thenReturn(false);
        when(informeRepository.save(any(InformeAdjunto.class))).thenReturn(informeMock);
        when(informeMapper.toDto(any(InformeAdjunto.class))).thenReturn(responseDTO);

        InformeAdjuntoResponseDTO result = informeService.adjuntar(dto);

        assertNotNull(result);
        verify(informeRepository).save(any(InformeAdjunto.class));
    }

    @Test
    @DisplayName("Adjuntar informe - Estudiante no encontrado (404)")
    void adjuntar_LanzaExcepcion_EstudianteNoEncontrado() {
        InformeAdjuntoRequestDTO dto = new InformeAdjuntoRequestDTO();
        dto.setIdUsuario(99);
        dto.setUrlArchivo("https://storage.example.com/informe.pdf");
        dto.setNombre("informe_2024");

        when(estudianteRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.adjuntar(dto));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(informeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Adjuntar informe - Nombre duplicado (409)")
    void adjuntar_LanzaExcepcion_NombreDuplicado() {
        InformeAdjuntoRequestDTO dto = new InformeAdjuntoRequestDTO();
        dto.setIdUsuario(1);
        dto.setUrlArchivo("https://storage.example.com/informe.pdf");
        dto.setNombre("informe_2024");

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));
        when(informeRepository.existsByEstudianteIdUsuarioAndNombre(1, "informe_2024")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.adjuntar(dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(informeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Adjuntar informe - Extensión inválida (400)")
    void adjuntar_LanzaExcepcion_ExtensionInvalida() {
        InformeAdjuntoRequestDTO dto = new InformeAdjuntoRequestDTO();
        dto.setIdUsuario(1);
        dto.setUrlArchivo("https://storage.example.com/informe.exe");
        dto.setNombre("informe_2024");

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));
        when(informeRepository.existsByEstudianteIdUsuarioAndNombre(1, "informe_2024")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.adjuntar(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(informeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Adjuntar informe - URL demasiado larga (400)")
    void adjuntar_LanzaExcepcion_UrlDemasiadoLarga() {
        InformeAdjuntoRequestDTO dto = new InformeAdjuntoRequestDTO();
        dto.setIdUsuario(1);
        dto.setUrlArchivo("https://storage.example.com/" + "a".repeat(480) + ".pdf");
        dto.setNombre("informe_2024");

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));
        when(informeRepository.existsByEstudianteIdUsuarioAndNombre(1, "informe_2024")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.adjuntar(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(informeRepository, never()).save(any());
    }

    // ── Obtener por ID ────────────────────────────────────────

    @Test
    @DisplayName("Obtener por ID - Éxito")
    void obtenerPorId_Exito() {
        when(informeRepository.findById(1)).thenReturn(Optional.of(informeMock));
        when(informeMapper.toDto(informeMock)).thenReturn(responseDTO);

        InformeAdjuntoResponseDTO result = informeService.obtenerPorId(1);

        assertNotNull(result);
        assertEquals(1, result.getIdInforme());
        verify(informeMapper).toDto(informeMock);
    }

    @Test
    @DisplayName("Obtener por ID - No encontrado (404)")
    void obtenerPorId_LanzaExcepcion_NoEncontrado() {
        when(informeRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.obtenerPorId(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(informeMapper, never()).toDto(any());
    }

    // ── Eliminar ──────────────────────────────────────────────

    @Test
    @DisplayName("Eliminar informe - Estado pasa a ELIMINADO")
    void eliminar_DebePonerEstadoEliminado() {
        when(informeRepository.findById(1)).thenReturn(Optional.of(informeMock));
        when(informeRepository.save(any(InformeAdjunto.class))).thenReturn(informeMock);

        informeService.eliminar(1);

        assertEquals(EstadoLogico.ELIMINADO, informeMock.getEstado());
        verify(informeRepository).save(informeMock);
        verify(informeRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Eliminar informe - No encontrado (404)")
    void eliminar_LanzaExcepcion_NoEncontrado() {
        when(informeRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.eliminar(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(informeRepository, never()).save(any());
    }

    // ── Listar ────────────────────────────────────────────────

    @Test
    @DisplayName("Listar por estudiante - Éxito")
    void listarPorEstudiante_Exito() {
        List<InformeAdjunto> informes = List.of(informeMock);
        when(informeRepository.findByEstudianteIdUsuarioAndEstado(1, EstadoLogico.ACTIVO))
                .thenReturn(informes);
        when(informeMapper.toDto(informeMock)).thenReturn(responseDTO);

        List<InformeAdjuntoResponseDTO> result = informeService.listarPorEstudiante(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(informeRepository).findByEstudianteIdUsuarioAndEstado(1, EstadoLogico.ACTIVO);
    }
}