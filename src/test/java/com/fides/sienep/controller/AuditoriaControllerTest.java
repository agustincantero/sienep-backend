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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
        when(auditoriaService.listarAuditorias(anyInt(), anyInt(), any(), any(), any(), any())).thenReturn(pagina);

        mockMvc.perform(get("/auditorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idAuditoria").value(1));
    }
}
