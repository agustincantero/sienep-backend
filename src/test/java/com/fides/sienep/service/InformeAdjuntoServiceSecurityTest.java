package com.fides.sienep.service;

import com.fides.sienep.dto.InformeAdjuntoRequestDTO;
import com.fides.sienep.mapper.InformeAdjuntoMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.InformeAdjuntoRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

// Las reglas de @PreAuthorize de InformeAdjuntoService no se evalúan en InformeAdjuntoServiceTest
// (Mockito puro, sin proxy de Spring). Acá se levanta solo el servicio con @EnableMethodSecurity y
// dependencias mockeadas, para verificar las expresiones SpEL del autoservicio del estudiante sin
// necesitar base de datos. "Pasa la seguridad" se comprueba porque la llamada llega al cuerpo del
// método (el repositorio mockeado no encuentra al estudiante y responde 404, no AccessDenied).
@SpringJUnitConfig(InformeAdjuntoServiceSecurityTest.Config.class)
class InformeAdjuntoServiceSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean InformeAdjuntoRepository informeRepository() { return mock(InformeAdjuntoRepository.class); }
        @Bean EstudianteRepository estudianteRepository() { return mock(EstudianteRepository.class); }
        @Bean InformeAdjuntoMapper informeMapper() { return mock(InformeAdjuntoMapper.class); }
        @Bean FileStorageService fileStorageService() { return mock(FileStorageService.class); }

        @Bean
        InformeAdjuntoService informeAdjuntoService(InformeAdjuntoRepository informeRepository,
                                                    EstudianteRepository estudianteRepository,
                                                    InformeAdjuntoMapper informeMapper,
                                                    FileStorageService fileStorageService) {
            return new InformeAdjuntoService(informeRepository, estudianteRepository, informeMapper, fileStorageService);
        }
    }

    @Autowired private InformeAdjuntoService informeService;
    @Autowired private InformeAdjuntoRepository informeRepository;

    @BeforeEach
    void setUp() {
        reset(informeRepository);
        when(informeRepository.findByEstudianteIdUsuarioAndEstado(anyInt(), eq(EstadoLogico.ACTIVO), any()))
                .thenReturn(Page.empty());
    }

    private InformeAdjuntoRequestDTO dtoPara(int idUsuario) {
        InformeAdjuntoRequestDTO dto = new InformeAdjuntoRequestDTO();
        dto.setIdUsuario(idUsuario);
        dto.setNombre("informe");
        return dto;
    }

    // ── Adjuntar ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = "5", roles = "ESTUDIANTE")
    @DisplayName("Adjuntar - El estudiante puede adjuntar a su propia historia sin ADJUNTAR_INFORME")
    void adjuntar_EstudiantePropio_PasaLaSeguridad() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.adjuntar(dtoPara(5)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @WithMockUser(username = "5", roles = "ESTUDIANTE")
    @DisplayName("Adjuntar - Un estudiante no puede adjuntar a la historia de otro")
    void adjuntar_EstudianteAjeno_AccessDenied() {
        assertThrows(AccessDeniedException.class, () -> informeService.adjuntar(dtoPara(6)));
    }

    @Test
    @WithMockUser(username = "10", authorities = "ADJUNTAR_INFORME")
    @DisplayName("Adjuntar - Un funcionario con ADJUNTAR_INFORME puede adjuntar a cualquier estudiante")
    void adjuntar_FuncionarioConPermiso_PasaLaSeguridad() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> informeService.adjuntar(dtoPara(5)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ── Listar ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "5", roles = "ESTUDIANTE")
    @DisplayName("Listar - El estudiante puede listar sus propios informes sin VER_INFORME")
    void listar_EstudiantePropio_Permitido() {
        assertDoesNotThrow(() -> informeService.listarPorEstudiante(5, PageRequest.of(0, 20)));
    }

    @Test
    @WithMockUser(username = "5", roles = "ESTUDIANTE")
    @DisplayName("Listar - Un estudiante no puede listar los informes de otro")
    void listar_EstudianteAjeno_AccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> informeService.listarPorEstudiante(6, PageRequest.of(0, 20)));
    }

    @Test
    @WithMockUser(username = "10", authorities = "VER_INFORME")
    @DisplayName("Listar - Un funcionario con VER_INFORME puede listar los de cualquier estudiante")
    void listar_FuncionarioConPermiso_Permitido() {
        assertDoesNotThrow(() -> informeService.listarPorEstudiante(6, PageRequest.of(0, 20)));
    }

    // ── Ver / eliminar ────────────────────────────────────────

    @Test
    @WithMockUser(username = "10")
    @DisplayName("Ver - Un funcionario sin VER_INFORME no puede ver informes")
    void obtenerPorId_FuncionarioSinPermiso_AccessDenied() {
        assertThrows(AccessDeniedException.class, () -> informeService.obtenerPorId(1));
    }

    @Test
    @WithMockUser(username = "5", roles = "ESTUDIANTE")
    @DisplayName("Eliminar - El estudiante no puede borrar informes, ni siquiera los propios")
    void eliminar_Estudiante_AccessDenied() {
        assertThrows(AccessDeniedException.class, () -> informeService.eliminar(1));
    }
}
