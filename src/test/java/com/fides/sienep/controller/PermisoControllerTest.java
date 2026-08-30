package com.fides.sienep.controller;

import com.fides.sienep.dto.PermisoResponseDTO;
import com.fides.sienep.dto.PermisoUpdateDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.PermisoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    @DisplayName("Editar descripción - Válida responde 200")
    void editarDescripcion_Valida_Responde200() throws Exception {
        PermisoUpdateDTO dto = new PermisoUpdateDTO();
        dto.setDescripcion("Nueva descripción");
        when(permisoService.editarDescripcion(eq(1), any())).thenReturn(responseDTO());

        mockMvc.perform(put("/permisos/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPermiso").value(1));
    }

    @Test
    @DisplayName("Editar descripción - Permiso inactivo responde 409 (propagado desde el service)")
    void editarDescripcion_PermisoInactivo_Responde409() throws Exception {
        PermisoUpdateDTO dto = new PermisoUpdateDTO();
        dto.setDescripcion("Nueva descripción");
        when(permisoService.editarDescripcion(eq(1), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "No se puede editar un permiso inactivo"));

        mockMvc.perform(put("/permisos/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }
}
