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
    @DisplayName("Registrar notificación - Sin autor (scheduler) guarda NOTIFICACION con el detalle del envío")
    void registrarNotificacion_SinAutor_GuardaDetalle() {
        Usuario destinatario = new Estudiante();
        destinatario.setIdUsuario(2);
        destinatario.setNombre("Juan");
        destinatario.setApellido("García");
        destinatario.setEmail("juan.garcia@utec.edu.uy");

        auditoriaService.registrarNotificacion("recordatorios", null, "RECORDATORIO_PROXIMO",
                "REC-2026-000001", destinatario, true, null);

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        Auditoria guardada = captor.getValue();
        assertEquals("RECORDATORIOS", guardada.getEntidad());
        assertEquals(TipoAccion.NOTIFICACION, guardada.getAccion());
        assertNull(guardada.getAutor());
        assertNull(guardada.getValorAnterior());
        assertEquals("EMAIL", guardada.getValorNuevo().get("canal").asText());
        assertEquals("RECORDATORIO_PROXIMO", guardada.getValorNuevo().get("tipo").asText());
        assertEquals("REC-2026-000001", guardada.getValorNuevo().get("id_negocio").asText());
        assertEquals(2, guardada.getValorNuevo().get("id_destinatario").asInt());
        assertEquals("Juan García", guardada.getValorNuevo().get("destinatario").asText());
        assertEquals("ENVIADA", guardada.getValorNuevo().get("resultado").asText());
        assertFalse(guardada.getValorNuevo().has("motivo"));
        // RD01 — el email del destinatario no queda en la auditoría.
        assertFalse(guardada.getValorNuevo().toString().contains("juan.garcia@utec.edu.uy"));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("Registrar notificación - Con error guarda el resultado ERROR, el motivo y el autor")
    void registrarNotificacion_ConError_GuardaMotivoYAutor() {
        Usuario autor = new Estudiante();
        autor.setIdUsuario(1);
        autor.setNombre("Ana");
        autor.setApellido("López");
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(autor));

        auditoriaService.registrarNotificacion("INSTANCIAS", 1, "INSTANCIA_CREADA",
                "INST-2026-000001", autor, false, "SMTP no disponible");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        Auditoria guardada = captor.getValue();
        assertSame(autor, guardada.getAutor());
        assertEquals("ERROR", guardada.getValorNuevo().get("resultado").asText());
        assertEquals("SMTP no disponible", guardada.getValorNuevo().get("motivo").asText());
    }

    @Test
    @DisplayName("Registrar notificación - Sin destinatario (relación rota) guarda igual el evento")
    void registrarNotificacion_SinDestinatario_GuardaIgual() {
        auditoriaService.registrarNotificacion("RECORDATORIOS", null, "RECORDATORIO_PROXIMO",
                "REC-2026-000001", null, false, "NullPointerException");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertFalse(captor.getValue().getValorNuevo().has("id_destinatario"));
        assertEquals("ERROR", captor.getValue().getValorNuevo().get("resultado").asText());
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

        Page<AuditoriaResponseDTO> resultado = auditoriaService.listarAuditorias(0, 10, "PERMISO", TipoAccion.INSERT, 1, null);

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

        Page<AuditoriaResponseDTO> resultado = auditoriaService.listarAuditorias(0, 10, null, null, null, null);

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
        auditoriaService.listarAuditorias(0, 10, "USUARIO", TipoAccion.LOGIN, 1, null);

        // Assert - Ejecutamos la lambda capturada para cubrirla
        Specification<Auditoria> spec = specCaptor.getValue();
        spec.toPredicate(root, query, cb);

        verify(cb).equal(pathEntidad, "USUARIO");
        verify(cb).equal(pathAccion, TipoAccion.LOGIN);
        verify(cb).equal(pathIdUsuario, 1);
    }

    @Test
    @DisplayName("Listar auditorías - filtro texto busca por nombre o apellido del autor")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void listarAuditorias_FiltroTexto_BuscaPorAutor() {
        // Arrange
        Root<Auditoria> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path pathAutor = mock(Path.class);
        Path pathNombre = mock(Path.class);
        Path pathApellido = mock(Path.class);
        Path pathDocumento = mock(Path.class);

        when(root.get("autor")).thenReturn(pathAutor);
        when(pathAutor.get("nombre")).thenReturn(pathNombre);
        when(pathAutor.get("apellido")).thenReturn(pathApellido);
        when(pathAutor.get("documento")).thenReturn(pathDocumento);
        when(cb.lower(pathNombre)).thenReturn(pathNombre);
        when(cb.lower(pathApellido)).thenReturn(pathApellido);
        when(cb.lower(pathDocumento)).thenReturn(pathDocumento);

        Page<Auditoria> pageMock = new PageImpl<>(List.of());
        ArgumentCaptor<Specification<Auditoria>> specCaptor = (ArgumentCaptor<Specification<Auditoria>>) (Object) ArgumentCaptor.forClass(Specification.class);

        when(auditoriaRepository.findAll(specCaptor.capture(), any(Pageable.class))).thenReturn(pageMock);

        // Act - solo el filtro texto, sin entidad/accion/idAutor
        auditoriaService.listarAuditorias(0, 10, null, null, null, "carlos");

        // Assert
        Specification<Auditoria> spec = specCaptor.getValue();
        spec.toPredicate(root, query, cb);

        verify(cb).isNotNull(pathAutor);
        verify(cb).like(pathNombre, "%carlos%");
        verify(cb).like(pathApellido, "%carlos%");
        verify(cb).like(pathDocumento, "%carlos%");
    }
}
