package com.fides.sienep.service;

import com.fides.sienep.dto.AuditoriaResponseDTO;
import com.fides.sienep.model.Auditoria;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.TipoAccion;
import com.fides.sienep.model.Usuario;
import com.fides.sienep.repository.AuditoriaRepository;
import com.fides.sienep.repository.UsuarioRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import com.fides.sienep.mapper.AuditoriaMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuditoriaServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @SuppressWarnings("unused")
    @Spy
    private AuditoriaMapper mapper = Mappers.getMapper(AuditoriaMapper.class);

    @InjectMocks
    private AuditoriaService auditoriaService;

    @Test
    @DisplayName("Registrar evento manual - Éxito")
    void registrarEventoManual_Exito() {
        Usuario autor = new Estudiante();
        autor.setIdUsuario(1);
        autor.setNombre("Juan");
        autor.setApellido("Perez");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(autor));

        auditoriaService.registrarEventoManual("ESTUDIANTE", TipoAccion.INSERT, 1, "192.168.0.1");

        verify(auditoriaRepository, times(1)).save(any(Auditoria.class));
    }

    @Test
    @DisplayName("Registrar evento manual - Usuario no encontrado (No guarda)")
    void registrarEventoManual_UsuarioNoEncontrado_NoGuardaNada() {
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        auditoriaService.registrarEventoManual("ESTUDIANTE", TipoAccion.INSERT, 99, "192.168.0.1");

        verify(auditoriaRepository, never()).save(any(Auditoria.class));
    }

    @Test
    @DisplayName("Obtener auditoría por ID - Éxito")
    void obtenerAuditoriaPorId_Exito() {
        Usuario autor = new Estudiante();
        autor.setIdUsuario(1);
        autor.setNombre("Ana");
        autor.setApellido("Gomez");

        Auditoria auditoria = new Auditoria();
        auditoria.setId(10);
        auditoria.setEntidad("USUARIO");
        auditoria.setAccion(TipoAccion.UPDATE);
        auditoria.setAutor(autor);
        auditoria.setIp("127.0.0.1");
        auditoria.setFechaHora(LocalDateTime.now());

        when(auditoriaRepository.findById(10)).thenReturn(Optional.of(auditoria));

        AuditoriaResponseDTO resultado = auditoriaService.obtenerAuditoriaPorId(10);

        assertNotNull(resultado);
        assertEquals(10, resultado.getIdAuditoria());
        assertEquals("USUARIO", resultado.getEntidad());
        assertEquals("Ana Gomez", resultado.getNombreAutor());
        assertEquals(1, resultado.getIdAutor());
    }

    @Test
    @DisplayName("Obtener auditoría por ID - No encontrada (404)")
    void obtenerAuditoriaPorId_LanzaNotFound() {
        when(auditoriaRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> auditoriaService.obtenerAuditoriaPorId(99));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Auditoria no encontrada con el ID: 99", ex.getReason());
    }

    @Test
    @DisplayName("Listar auditorías paginadas con filtros - Éxito")
    void listarAuditorias_Exito() {
        Usuario autor = new Estudiante();
        autor.setIdUsuario(1);
        autor.setNombre("Carlos");
        autor.setApellido("Rodriguez");

        Auditoria aud = new Auditoria();
        aud.setId(1);
        aud.setEntidad("PERMISO");
        aud.setAccion(TipoAccion.INSERT);
        aud.setAutor(autor);

        Page<Auditoria> pageMock = new PageImpl<>(List.of(aud), PageRequest.of(0, 10), 1);

        when(auditoriaRepository.findAll(Mockito.<Specification<Auditoria>>any(), any(Pageable.class))).thenReturn(pageMock);

        Page<AuditoriaResponseDTO> resultado = auditoriaService.listarAuditorias(0, 10, "PERMISO", TipoAccion.INSERT, 1);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals("PERMISO", resultado.getContent().get(0).getEntidad());
        assertEquals("Carlos Rodriguez", resultado.getContent().get(0).getNombreAutor());
    }

    @Test
    @DisplayName("Listar auditorías sin filtros - Éxito")
    void listarAuditorias_SinFiltros_Exito() {
        Page<Auditoria> pageMock = new PageImpl<>(List.of());
        when(auditoriaRepository.findAll(Mockito.<Specification<Auditoria>>any(), any(Pageable.class)))
            .thenReturn(pageMock);

        Page<AuditoriaResponseDTO> resultado = auditoriaService.listarAuditorias(0, 10, null, null, null);

        assertNotNull(resultado);
    }

    @Test
    @DisplayName("Listar auditorías - Cobertura de Specification")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void listarAuditorias_Specification_Cobertura() {
        // Arrange
        Root<Auditoria> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path pathEntidad = mock(Path.class);
        Path pathAccion = mock(Path.class);
        Path pathAutor = mock(Path.class);
        Path pathIdUsuario = mock(Path.class);

        when(root.get("entidad")).thenReturn(pathEntidad);
        when(root.get("accion")).thenReturn(pathAccion);
        when(root.get("autor")).thenReturn(pathAutor);
        when(pathAutor.get("idUsuario")).thenReturn(pathIdUsuario);

        Page<Auditoria> pageMock = new PageImpl<>(List.of());
        ArgumentCaptor<Specification<Auditoria>> specCaptor = (ArgumentCaptor<Specification<Auditoria>>) (Object) ArgumentCaptor.forClass(Specification.class);
        
        when(auditoriaRepository.findAll(specCaptor.capture(), any(Pageable.class))).thenReturn(pageMock);

        // Act
        auditoriaService.listarAuditorias(0, 10, "USUARIO", TipoAccion.LOGIN, 1);

        // Assert - Ejecutamos la lambda capturada para cubrirla
        Specification<Auditoria> spec = specCaptor.getValue();
        spec.toPredicate(root, query, cb);

        verify(cb).equal(pathEntidad, "USUARIO");
        verify(cb).equal(pathAccion, TipoAccion.LOGIN);
        verify(cb).equal(pathIdUsuario, 1);
    }
}
