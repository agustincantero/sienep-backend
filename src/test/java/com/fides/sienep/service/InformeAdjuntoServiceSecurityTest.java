package com.fides.sienep.service;

import com.fides.sienep.dto.InformeAdjuntoRequestDTO;
import com.fides.sienep.mapper.InformeAdjuntoMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.InformeAdjuntoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Reglas de @PreAuthorize de InformeAdjuntoService con @EnableMethodSecurity real (en InformeAdjuntoServiceTest,
// con Mockito puro, no se evalúan). Lo importante: el estudiante solo puede usar sus endpoints propios (/mios),
// y los de funcionarios no tienen ninguna excepción para él (no puede subir, ver por id ni borrar).
@SpringJUnitConfig(InformeAdjuntoServiceSecurityTest.Config.class)
class InformeAdjuntoServiceSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        InformeAdjuntoService informeAdjuntoService() {
            InformeAdjuntoRepository repo = mock(InformeAdjuntoRepository.class);
            when(repo.findByEstudianteIdUsuarioAndEstado(anyInt(), eq(EstadoLogico.ACTIVO), any()))
                    .thenReturn(Page.empty());
            return new InformeAdjuntoService(repo, mock(EstudianteRepository.class),
                    mock(InformeAdjuntoMapper.class), mock(FileStorageService.class));
        }
    }

    @Autowired private InformeAdjuntoService informeService;

    private InformeAdjuntoRequestDTO dtoPara(int idUsuario) {
        InformeAdjuntoRequestDTO dto = new InformeAdjuntoRequestDTO();
        dto.setIdUsuario(idUsuario);
        dto.setNombre("informe");
        return dto;
    }

    // ── Estudiante ────────────────────────────────────────────

    @Test
    @WithMockUser(username = "5", roles = "ESTUDIANTE")
    @DisplayName("Estudiante - Puede listar sus propios informes")
    void estudiante_ListarMios_Permitido() {
        assertDoesNotThrow(() -> informeService.listarMios(PageRequest.of(0, 20)));
    }

    @Test
    @WithMockUser(username = "5", roles = "ESTUDIANTE")
    @DisplayName("Estudiante - No puede subir informes, ni siquiera a su propia historia")
    void estudiante_Adjuntar_AccessDenied() {
        assertThrows(AccessDeniedException.class, () -> informeService.adjuntar(dtoPara(5)));
    }

    @Test
    @WithMockUser(username = "5", roles = "ESTUDIANTE")
    @DisplayName("Estudiante - No puede usar los endpoints de funcionarios (listar por id, ver, descargar, borrar)")
    void estudiante_EndpointsDeFuncionarios_AccessDenied() {
        assertThrows(AccessDeniedException.class, () -> informeService.listarPorEstudiante(5, PageRequest.of(0, 20)));
        assertThrows(AccessDeniedException.class, () -> informeService.obtenerPorId(1));
        assertThrows(AccessDeniedException.class, () -> informeService.descargar(1));
        assertThrows(AccessDeniedException.class, () -> informeService.eliminar(1));
    }

    // ── Funcionario ───────────────────────────────────────────

    @Test
    @WithMockUser(username = "10", authorities = {"VER_INFORME", "ADJUNTAR_INFORME"})
    @DisplayName("Funcionario - Los endpoints propios del estudiante no son para él, aunque tenga VER_INFORME")
    void funcionario_EndpointsDelEstudiante_AccessDenied() {
        assertThrows(AccessDeniedException.class, () -> informeService.listarMios(PageRequest.of(0, 20)));
        assertThrows(AccessDeniedException.class, () -> informeService.descargarMio(1));
    }

    @Test
    @WithMockUser(username = "10", authorities = "VER_INFORME")
    @DisplayName("Funcionario - Con VER_INFORME lista los de cualquier estudiante, como antes")
    void funcionario_ListarPorEstudiante_Permitido() {
        assertDoesNotThrow(() -> informeService.listarPorEstudiante(6, PageRequest.of(0, 20)));
    }
}
