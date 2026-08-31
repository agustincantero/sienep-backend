package com.fides.sienep.controller;

import com.fides.sienep.dto.InvolucradoRequestDTO;
import com.fides.sienep.dto.InvolucradoResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.InvolucradoService;
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
@WebMvcTest(InvolucradoController.class)
class InvolucradoControllerTest extends AbstractControllerTest {

    @MockitoBean private InvolucradoService involucradoService;

    private InvolucradoResponseDTO responseDTO() {
        return InvolucradoResponseDTO.builder()
                .codInstancia(1)
                .nomInvolucrado("Padre del estudiante")
                .estado(EstadoLogico.ACTIVO)
                .build();
    }

    @Test
    @DisplayName("Agregar - Datos válidos responde 201")
    void agregar_Valido_Responde201() throws Exception {
        InvolucradoRequestDTO dto = new InvolucradoRequestDTO();
        dto.setNomInvolucrado("Padre del estudiante");
        when(involucradoService.agregar(eq(1), any())).thenReturn(responseDTO());

        mockMvc.perform(post("/incidencias/{codInstancia}/involucrados", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomInvolucrado").value("Padre del estudiante"));
    }

    @Test
    @DisplayName("Agregar - Nombre en blanco responde 400 (Bean Validation)")
    void agregar_NombreEnBlanco_Responde400() throws Exception {
        InvolucradoRequestDTO dto = new InvolucradoRequestDTO();
        dto.setNomInvolucrado("   ");

        mockMvc.perform(post("/incidencias/{codInstancia}/involucrados", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Agregar - Ya existe en la instancia responde 409 (propagado desde el service)")
    void agregar_YaExiste_Responde409() throws Exception {
        InvolucradoRequestDTO dto = new InvolucradoRequestDTO();
        dto.setNomInvolucrado("Padre del estudiante");
        when(involucradoService.agregar(eq(1), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Involucrado ya existe en esta instancia"));

        mockMvc.perform(post("/incidencias/{codInstancia}/involucrados", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Listar - Responde 200 con la lista del service")
    void listar_Responde200() throws Exception {
        when(involucradoService.listar(1)).thenReturn(List.of(responseDTO()));

        mockMvc.perform(get("/incidencias/{codInstancia}/involucrados", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomInvolucrado").value("Padre del estudiante"));
    }

    @Test
    @DisplayName("Desactivar - Responde 204")
    void desactivar_Responde204() throws Exception {
        mockMvc.perform(delete("/incidencias/{codInstancia}/involucrados/{nomInvolucrado}", 1, "Padre del estudiante"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Desactivar - Inexistente responde 404 (propagado desde el service)")
    void desactivar_Inexistente_Responde404() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Involucrado no encontrado"))
                .when(involucradoService).desactivar(1, "Nadie");

        mockMvc.perform(delete("/incidencias/{codInstancia}/involucrados/{nomInvolucrado}", 1, "Nadie"))
                .andExpect(status().isNotFound());
    }
}
