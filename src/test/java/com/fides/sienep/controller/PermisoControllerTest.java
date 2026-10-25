package com.fides.sienep.controller;

import com.fides.sienep.dto.PermisoResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.PermisoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// P1 de la auditoría — mismo criterio que CarreraControllerTest (ver ese archivo
// y AbstractControllerTest para el porqué del diseño).
@WebMvcTest(PermisoController.class)
class PermisoControllerTest extends AbstractControllerTest {

    @MockitoBean private PermisoService permisoService;

    private PermisoResponseDTO responseDTO() {
        return PermisoResponseDTO.builder()
                .idPermiso(1)
                .nombre("VER_ESTUDIANTE")
                .descripcion("Ver ficha de estudiante")
                .estado(EstadoLogico.ACTIVO)
                .build();
    }

    @Test
    @DisplayName("Listar - Responde 200 con la lista del service")
    void listar_Responde200() throws Exception {
        when(permisoService.listar()).thenReturn(List.of(responseDTO()));

        mockMvc.perform(get("/permisos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPermiso").value(1));
    }
}
