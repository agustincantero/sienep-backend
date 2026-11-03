package com.fides.sienep.service;

import com.fides.sienep.dto.ArchivoDescargaDTO;
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
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InformeAdjuntoServiceTest {

    @Mock private InformeAdjuntoRepository informeRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private InformeAdjuntoMapper informeMapper;
    @Mock private FileStorageService fileStorageService;

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
                .urlArchivo("3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.pdf")
                .ruta("informes/1")
                .nombre("informe_2024")
                .estado(EstadoLogico.ACTIVO)
                .build();

        responseDTO = InformeAdjuntoResponseDTO.builder()
                .idInforme(1)
                .idUsuario(1)
                .nombre("informe_2024")
                .urlDescarga("/api/informes-adjuntos/1/descargar")
                .estado(EstadoLogico.ACTIVO)
                .build();
    }

    private InformeAdjuntoRequestDTO buildDto(Integer idUsuario, String nombre, MultipartFile archivo) {
        InformeAdjuntoRequestDTO dto = new InformeAdjuntoRequestDTO();
        dto.setIdUsuario(idUsuario);
        dto.setNombre(nombre);
        dto.setArchivo(archivo);
        return dto;
    }

    // ── Adjuntar ──────────────────────────────────────────────

    @Test
    @DisplayName("Adjuntar informe - Éxito")
    void adjuntar_Exito() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "informe.pdf", "application/pdf", "contenido".getBytes());
        InformeAdjuntoRequestDTO dto = buildDto(1, "informe_2024", archivo);

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));
        when(informeRepository.existsByEstudianteIdUsuarioAndNombreAndEstadoNot(1, "informe_2024", EstadoLogico.ELIMINADO)).thenReturn(false);
        when(fileStorageService.guardar(eq(archivo), eq("informes/1"), anyList(), anyLong()))
                .thenReturn("3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.pdf");
        when(informeRepository.save(any(InformeAdjunto.class))).thenReturn(informeMock);
        when(informeMapper.toDto(any(InformeAdjunto.class))).thenReturn(responseDTO);

        InformeAdjuntoResponseDTO result = informeService.adjuntar(dto);

        assertNotNull(result);
        assertEquals("informe_2024", result.getNombre());
        verify(informeRepository).save(any(InformeAdjunto.class));
    }

    @Test
    @DisplayName("Adjuntar informe - Estudiante no encontrado (404)")
    void adjuntar_LanzaExcepcion_EstudianteNoEncontrado() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "informe.pdf", "application/pdf", "contenido".getBytes());
        InformeAdjuntoRequestDTO dto = buildDto(99, "informe_2024", archivo);

        when(estudianteRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.adjuntar(dto));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(informeRepository, never()).save(any());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("Adjuntar informe - Estudiante inactivo (422)")
    void adjuntar_LanzaExcepcion_EstudianteInactivo() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "informe.pdf", "application/pdf", "contenido".getBytes());
        InformeAdjuntoRequestDTO dto = buildDto(1, "informe_2024", archivo);
        estudianteMock.setEstado(EstadoLogico.INACTIVO);

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.adjuntar(dto));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(informeRepository, never()).save(any());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("Adjuntar informe - Estudiante eliminado (422)")
    void adjuntar_LanzaExcepcion_EstudianteEliminado() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "informe.pdf", "application/pdf", "contenido".getBytes());
        InformeAdjuntoRequestDTO dto = buildDto(1, "informe_2024", archivo);
        estudianteMock.setEstado(EstadoLogico.ELIMINADO);

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.adjuntar(dto));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(informeRepository, never()).save(any());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("Adjuntar informe - Estudiante pendiente de activación - Éxito")
    void adjuntar_Exito_EstudiantePendienteDeActivacion() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "informe.pdf", "application/pdf", "contenido".getBytes());
        InformeAdjuntoRequestDTO dto = buildDto(1, "informe_2024", archivo);
        estudianteMock.setEstado(EstadoLogico.PENDIENTE_DE_ACTIVACION);

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));
        when(informeRepository.existsByEstudianteIdUsuarioAndNombreAndEstadoNot(1, "informe_2024", EstadoLogico.ELIMINADO)).thenReturn(false);
        when(fileStorageService.guardar(eq(archivo), eq("informes/1"), anyList(), anyLong()))
                .thenReturn("3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.pdf");
        when(informeRepository.save(any(InformeAdjunto.class))).thenReturn(informeMock);
        when(informeMapper.toDto(any(InformeAdjunto.class))).thenReturn(responseDTO);

        assertNotNull(informeService.adjuntar(dto));
        verify(informeRepository).save(any(InformeAdjunto.class));
    }

    @Test
    @DisplayName("Adjuntar informe - Nombre duplicado (409)")
    void adjuntar_LanzaExcepcion_NombreDuplicado() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "informe.pdf", "application/pdf", "contenido".getBytes());
        InformeAdjuntoRequestDTO dto = buildDto(1, "informe_2024", archivo);

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));
        when(informeRepository.existsByEstudianteIdUsuarioAndNombreAndEstadoNot(1, "informe_2024", EstadoLogico.ELIMINADO)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.adjuntar(dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(informeRepository, never()).save(any());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("Adjuntar informe - El nombre de un informe eliminado se puede reutilizar")
    void adjuntar_Exito_NombreDeInformeEliminado() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "informe.pdf", "application/pdf", "contenido".getBytes());
        InformeAdjuntoRequestDTO dto = buildDto(1, "informe_2024", archivo);

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));
        // El chequeo excluye ELIMINADO: es lo que deja pasar un nombre que solo usa un informe dado de baja.
        when(informeRepository.existsByEstudianteIdUsuarioAndNombreAndEstadoNot(1, "informe_2024", EstadoLogico.ELIMINADO))
                .thenReturn(false);
        when(fileStorageService.guardar(eq(archivo), eq("informes/1"), anyList(), anyLong()))
                .thenReturn("3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.pdf");
        when(informeRepository.save(any(InformeAdjunto.class))).thenReturn(informeMock);
        when(informeMapper.toDto(any(InformeAdjunto.class))).thenReturn(responseDTO);

        assertNotNull(informeService.adjuntar(dto));
        verify(informeRepository).save(any(InformeAdjunto.class));
    }

    @Test
    @DisplayName("Adjuntar informe - Propaga el error de FileStorageService y no persiste nada")
    void adjuntar_PropagaExcepcionDeFileStorageService() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "informe.exe", "application/octet-stream", "contenido".getBytes());
        InformeAdjuntoRequestDTO dto = buildDto(1, "informe_2024", archivo);

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));
        when(informeRepository.existsByEstudianteIdUsuarioAndNombreAndEstadoNot(1, "informe_2024", EstadoLogico.ELIMINADO)).thenReturn(false);
        when(fileStorageService.guardar(eq(archivo), eq("informes/1"), anyList(), anyLong()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "La extensión del archivo no está permitida"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.adjuntar(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(informeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Adjuntar informe - Si el save() falla, limpia el archivo físico recién escrito")
    void adjuntar_LimpiaArchivoFisico_SiFallaElSave() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "informe.pdf", "application/pdf", "contenido".getBytes());
        InformeAdjuntoRequestDTO dto = buildDto(1, "informe_2024", archivo);

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteMock));
        when(informeRepository.existsByEstudianteIdUsuarioAndNombreAndEstadoNot(1, "informe_2024", EstadoLogico.ELIMINADO)).thenReturn(false);
        when(fileStorageService.guardar(eq(archivo), eq("informes/1"), anyList(), anyLong()))
                .thenReturn("3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.pdf");
        when(informeRepository.save(any(InformeAdjunto.class)))
                .thenThrow(new DataIntegrityViolationException("constraint violation"));

        assertThrows(DataIntegrityViolationException.class, () -> informeService.adjuntar(dto));

        verify(fileStorageService).eliminarSiExiste("informes/1/3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.pdf");
    }

    // ── Descargar ─────────────────────────────────────────────

    @Test
    @DisplayName("Descargar - Éxito")
    void descargar_Exito() {
        Resource recursoMock = mock(Resource.class);
        when(informeRepository.findByIdInformeAndEstado(1, EstadoLogico.ACTIVO)).thenReturn(Optional.of(informeMock));
        when(fileStorageService.cargar("informes/1/3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.pdf"))
                .thenReturn(recursoMock);
        when(fileStorageService.extraerExtension(informeMock.getUrlArchivo())).thenReturn("pdf");
        when(fileStorageService.mediaTypeDeExtension("pdf")).thenReturn(MediaType.APPLICATION_PDF);

        ArchivoDescargaDTO resultado = informeService.descargar(1);

        assertNotNull(resultado);
        assertEquals("informe_2024.pdf", resultado.getNombreDescarga());
        assertEquals(MediaType.APPLICATION_PDF, resultado.getContentType());
        assertSame(recursoMock, resultado.getRecurso());
    }

    @Test
    @DisplayName("Descargar - Informe no encontrado (404)")
    void descargar_LanzaExcepcion_InformeNoEncontrado() {
        when(informeRepository.findByIdInformeAndEstado(99, EstadoLogico.ACTIVO)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.descargar(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("Descargar - Archivo físico faltante (404)")
    void descargar_LanzaExcepcion_ArchivoFisicoFaltante() {
        when(informeRepository.findByIdInformeAndEstado(1, EstadoLogico.ACTIVO)).thenReturn(Optional.of(informeMock));
        when(fileStorageService.cargar(anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo físico no se encuentra disponible"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.descargar(1));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ── Eliminar ──────────────────────────────────────────────

    @Test
    @DisplayName("Eliminar informe - Estado pasa a ELIMINADO")
    void eliminar_DebePonerEstadoEliminado() {
        when(informeRepository.findByIdInformeAndEstado(1, EstadoLogico.ACTIVO)).thenReturn(Optional.of(informeMock));
        when(informeRepository.save(any(InformeAdjunto.class))).thenReturn(informeMock);

        informeService.eliminar(1);

        assertEquals(EstadoLogico.ELIMINADO, informeMock.getEstado());
        verify(informeRepository).save(informeMock);
        verify(informeRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Eliminar informe - No encontrado (404)")
    void eliminar_LanzaExcepcion_NoEncontrado() {
        when(informeRepository.findByIdInformeAndEstado(99, EstadoLogico.ACTIVO)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.eliminar(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(informeRepository, never()).save(any());
    }

    // ── Listar ────────────────────────────────────────────────

    @Test
    @DisplayName("Listar por estudiante - Éxito, paginado (H18)")
    void listarPorEstudiante_Exito() {
        Pageable pageable = PageRequest.of(0, 20);
        when(informeRepository.findByEstudianteIdUsuarioAndEstadoIn(
                1, List.of(EstadoLogico.ACTIVO, EstadoLogico.ELIMINADO), pageable))
                .thenReturn(new PageImpl<>(List.of(informeMock), pageable, 1));
        when(informeMapper.toDtoList(List.of(informeMock))).thenReturn(List.of(responseDTO));

        Page<InformeAdjuntoResponseDTO> result = informeService.listarPorEstudiante(1, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        // Los ELIMINADO también se listan, como marca de que existieron.
        verify(informeRepository).findByEstudianteIdUsuarioAndEstadoIn(
                1, List.of(EstadoLogico.ACTIVO, EstadoLogico.ELIMINADO), pageable);
    }

    // ── Autoservicio del estudiante ───────────────────────────

    private void autenticarEstudiante(String idUsuario) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                idUsuario, null, List.of(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"))));
    }

    @Test
    @DisplayName("Mis informes - Lista los del estudiante del token, con la ruta de descarga propia")
    void listarMios_UsaElIdDelTokenYElMapperDelEstudiante() {
        autenticarEstudiante("5");
        Pageable pageable = PageRequest.of(0, 20);
        when(informeRepository.findByEstudianteIdUsuarioAndEstado(5, EstadoLogico.ACTIVO, pageable))
                .thenReturn(new PageImpl<>(List.of(informeMock)));
        when(informeMapper.toDtoDelEstudiante(informeMock)).thenReturn(responseDTO);

        Page<InformeAdjuntoResponseDTO> resultado = informeService.listarMios(pageable);

        assertEquals(1, resultado.getTotalElements());
        verify(informeMapper, never()).toDto(any());
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Descargar mío - Informe propio: descarga")
    void descargarMio_Propio() {
        autenticarEstudiante("5");
        Resource recursoMock = mock(Resource.class);
        when(informeRepository.findByIdInformeAndEstudianteIdUsuarioAndEstado(1, 5, EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(informeMock));
        when(fileStorageService.cargar("informes/1/3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.pdf")).thenReturn(recursoMock);
        when(fileStorageService.extraerExtension(informeMock.getUrlArchivo())).thenReturn("pdf");
        when(fileStorageService.mediaTypeDeExtension("pdf")).thenReturn(MediaType.APPLICATION_PDF);

        ArchivoDescargaDTO resultado = informeService.descargarMio(1);

        assertSame(recursoMock, resultado.getRecurso());
        assertEquals("informe_2024.pdf", resultado.getNombreDescarga());
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Descargar mío - Informe de otro estudiante: 404, sin revelar que existe")
    void descargarMio_Ajeno_404() {
        autenticarEstudiante("6");
        when(informeRepository.findByIdInformeAndEstudianteIdUsuarioAndEstado(1, 6, EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> informeService.descargarMio(1));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(fileStorageService);
        SecurityContextHolder.clearContext();
    }
}
