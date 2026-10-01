package com.fides.sienep.service;

import com.fides.sienep.dto.AtenderSolicitudInstanciaDTO;
import com.fides.sienep.dto.RechazarSolicitudInstanciaDTO;
import com.fides.sienep.dto.SolicitudInstanciaRequestDTO;
import com.fides.sienep.mapper.SolicitudInstanciaMapper;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.InstanciaComunRepository;
import com.fides.sienep.repository.SolicitudInstanciaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Reglas de @PreAuthorize de SolicitudInstanciaService con @EnableMethodSecurity real (en
// SolicitudInstanciaServiceTest, con Mockito puro, no se evalúan). Mismo esquema que
// InformeAdjuntoServiceSecurityTest: "pasa la seguridad" = llega al cuerpo del método y el repositorio
// mockeado no encuentra la solicitud/estudiante (404), en vez de AccessDenied.
@SpringJUnitConfig(SolicitudInstanciaSecurityTest.Config.class)
class SolicitudInstanciaSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean SolicitudInstanciaRepository solicitudRepository() {
            SolicitudInstanciaRepository repo = mock(SolicitudInstanciaRepository.class);
            when(repo.findByEstado(any(), any())).thenReturn(Page.empty());
            when(repo.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(Page.empty());
            when(repo.findByEstudianteIdUsuario(org.mockito.ArgumentMatchers.anyInt(), any())).thenReturn(Page.empty());
            return repo;
        }

        @Bean
        SolicitudInstanciaService solicitudInstanciaService(SolicitudInstanciaRepository solicitudRepository) {
            return new SolicitudInstanciaService(solicitudRepository, mock(EstudianteRepository.class),
                    mock(FuncionarioRepository.class), mock(InstanciaComunRepository.class),
                    mock(InstanciaComunService.class), mock(SolicitudInstanciaMapper.class));
        }
    }

    @Autowired private SolicitudInstanciaService solicitudService;

    private void assertPasaLaSeguridad(Runnable llamada) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, llamada::run);
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    private SolicitudInstanciaRequestDTO request() {
        SolicitudInstanciaRequestDTO dto = new SolicitudInstanciaRequestDTO();
        dto.setMotivo("Quiero una reunión");
        return dto;
    }

    // ── Estudiante ────────────────────────────────────────────

    @Test
    @WithMockUser(username = "5", roles = "ESTUDIANTE")
    @DisplayName("Crear - Un estudiante puede solicitar")
    void crear_Estudiante_PasaLaSeguridad() {
        assertPasaLaSeguridad(() -> solicitudService.crear(request()));
    }

    @Test
    @WithMockUser(username = "10", authorities = {"VER_SOLICITUDES_INSTANCIA", "ATENDER_SOLICITUD_INSTANCIA"})
    @DisplayName("Crear - Un funcionario no puede solicitar")
    void crear_Funcionario_AccessDenied() {
        assertThrows(AccessDeniedException.class, () -> solicitudService.crear(request()));
    }

    @Test
    @WithMockUser(username = "5", roles = "ESTUDIANTE")
    @DisplayName("Mis solicitudes - El estudiante puede listar las suyas")
    void listarMias_Estudiante_Permitido() {
        assertDoesNotThrow(() -> solicitudService.listarMias(PageRequest.of(0, 20)));
    }

    @Test
    @WithMockUser(username = "5", roles = "ESTUDIANTE")
    @DisplayName("Listar todas - Un estudiante no puede")
    void listar_Estudiante_AccessDenied() {
        assertThrows(AccessDeniedException.class, () -> solicitudService.listar(null, PageRequest.of(0, 20)));
    }

    @Test
    @WithMockUser(username = "5", roles = "ESTUDIANTE")
    @DisplayName("Atender / rechazar - Un estudiante no puede")
    void resolver_Estudiante_AccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> solicitudService.atender(1, new AtenderSolicitudInstanciaDTO()));
        assertThrows(AccessDeniedException.class,
                () -> solicitudService.rechazar(1, new RechazarSolicitudInstanciaDTO()));
    }

    // ── Funcionario ───────────────────────────────────────────

    @Test
    @WithMockUser(username = "10", authorities = "VER_SOLICITUDES_INSTANCIA")
    @DisplayName("Listar todas - Con VER_SOLICITUDES_INSTANCIA")
    void listar_ConPermiso_Permitido() {
        assertDoesNotThrow(() -> solicitudService.listar(null, PageRequest.of(0, 20)));
    }

    @Test
    @WithMockUser(username = "10")
    @DisplayName("Ver una - Un funcionario sin VER_SOLICITUDES_INSTANCIA no puede")
    void obtenerPorId_FuncionarioSinPermiso_AccessDenied() {
        assertThrows(AccessDeniedException.class, () -> solicitudService.obtenerPorId(1));
    }

    @Test
    @WithMockUser(username = "10", authorities = {"ATENDER_SOLICITUD_INSTANCIA", "CREAR_INSTANCIA"})
    @DisplayName("Atender - Con ATENDER_SOLICITUD_INSTANCIA y CREAR_INSTANCIA")
    void atender_ConAmbosPermisos_PasaLaSeguridad() {
        assertPasaLaSeguridad(() -> solicitudService.atender(1, new AtenderSolicitudInstanciaDTO()));
    }

    @Test
    @WithMockUser(username = "10", authorities = "ATENDER_SOLICITUD_INSTANCIA")
    @DisplayName("Atender - Sin CREAR_INSTANCIA no puede (atender crea una instancia)")
    void atender_SinCrearInstancia_AccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> solicitudService.atender(1, new AtenderSolicitudInstanciaDTO()));
    }

    @Test
    @WithMockUser(username = "10", authorities = "ATENDER_SOLICITUD_INSTANCIA")
    @DisplayName("Rechazar - Con ATENDER_SOLICITUD_INSTANCIA alcanza")
    void rechazar_ConPermiso_PasaLaSeguridad() {
        assertPasaLaSeguridad(() -> solicitudService.rechazar(1, new RechazarSolicitudInstanciaDTO()));
    }
}
