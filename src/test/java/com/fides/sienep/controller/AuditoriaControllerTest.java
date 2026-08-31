package com.fides.sienep.controller;

import com.fides.sienep.dto.AuditoriaResponseDTO;
import com.fides.sienep.model.TipoAccion;
import com.fides.sienep.service.AuditoriaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// P1 de la auditoría — mismo criterio que CarreraControllerTest (ver ese archivo
// y AbstractControllerTest para el porqué del diseño).
@WebMvcTest(AuditoriaController.class)
class AuditoriaControllerTest extends AbstractControllerTest {

    @MockitoBean private AuditoriaService auditoriaService;

    private AuditoriaResponseDTO responseDTO() {
        return AuditoriaResponseDTO.builder()
                .idAuditoria(1)
                .entidad("Estudiante")
                .accion(TipoAccion.INSERT)
                .build();
    }

    @Test
    @DisplayName("Listar - Responde 200 con la página del service")
    void listar_Responde200() throws Exception {
        Page<AuditoriaResponseDTO> pagina = new PageImpl<>(List.of(responseDTO()), PageRequest.of(0, 20), 1);
        when(auditoriaService.listarAuditorias(anyInt(), anyInt(), any(), any(), any())).thenReturn(pagina);

        mockMvc.perform(get("/auditorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idAuditoria").value(1));
    }

    @Test
    @DisplayName("Obtener por ID - Existente responde 200")
    void obtenerPorId_Existente_Responde200() throws Exception {
        when(auditoriaService.obtenerAuditoriaPorId(1)).thenReturn(responseDTO());

        mockMvc.perform(get("/auditorias/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAuditoria").value(1));
    }

    @Test
    @DisplayName("Obtener por ID - Inexistente responde 404")
    void obtenerPorId_Inexistente_Responde404() throws Exception {
        when(auditoriaService.obtenerAuditoriaPorId(99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Auditoria no encontrada con el ID: 99"));

        mockMvc.perform(get("/auditorias/{id}", 99))
                .andExpect(status().isNotFound());
    }
}
