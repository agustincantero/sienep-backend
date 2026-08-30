package com.fides.sienep.controller;

import com.fides.sienep.dto.GrupoRequestDTO;
import com.fides.sienep.dto.GrupoResponseDTO;
import com.fides.sienep.dto.GrupoUpdateDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.GrupoService;
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
@WebMvcTest(GrupoController.class)
class GrupoControllerTest extends AbstractControllerTest {

    @MockitoBean private GrupoService grupoService;

    private GrupoRequestDTO requestValido() {
        GrupoRequestDTO dto = new GrupoRequestDTO();
        dto.setNomGrupo("Grupo A");
        dto.setIdCarrera(1);
        dto.setGeneracion(2026);
        return dto;
    }

    private GrupoResponseDTO responseDTO() {
        return GrupoResponseDTO.builder()
                .idGrupo(1)
                .nomGrupo("Grupo A")
                .generacion(2026)
                .estado(EstadoLogico.ACTIVO)
                .build();
    }

    @Test
    @DisplayName("Crear - Datos válidos responde 201")
    void crear_Valido_Responde201() throws Exception {
        when(grupoService.crear(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idGrupo").value(1));
    }

    @Test
    @DisplayName("Crear - Nombre en blanco responde 400 (Bean Validation)")
    void crear_NombreEnBlanco_Responde400() throws Exception {
        GrupoRequestDTO dto = requestValido();
        dto.setNomGrupo("   ");

        mockMvc.perform(post("/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Carrera inactiva responde 409 (propagado desde el service)")
    void crear_CarreraInactiva_Responde409() throws Exception {
        when(grupoService.crear(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "No se puede crear un grupo para una carrera inactiva."));

        mockMvc.perform(post("/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Obtener por ID - Existente responde 200")
    void obtenerPorId_Existente_Responde200() throws Exception {
        when(grupoService.obtenerPorId(1)).thenReturn(responseDTO());

        mockMvc.perform(get("/grupos/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idGrupo").value(1));
    }

    @Test
    @DisplayName("Obtener por ID - Inexistente responde 404")
    void obtenerPorId_Inexistente_Responde404() throws Exception {
        when(grupoService.obtenerPorId(99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo no encontrado."));

        mockMvc.perform(get("/grupos/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Editar - Grupo no activo responde 409 (propagado desde el service)")
    void editar_GrupoNoActivo_Responde409() throws Exception {
        GrupoUpdateDTO dto = new GrupoUpdateDTO();
        dto.setNomGrupo("Grupo A");
        dto.setGeneracion(2026);
        when(grupoService.editar(eq(1), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "No se puede editar un grupo que no está ACTIVO."));

        mockMvc.perform(put("/grupos/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Listar - Responde 200 con la lista del service")
    void listar_Responde200() throws Exception {
        when(grupoService.listar(any(), any(), eq(EstadoLogico.ACTIVO))).thenReturn(List.of(responseDTO()));

        mockMvc.perform(get("/grupos").param("estado", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idGrupo").value(1));
    }

    @Test
    @DisplayName("Desactivar - Responde 204")
    void desactivar_Responde204() throws Exception {
        mockMvc.perform(delete("/grupos/{id}", 1))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Reactivar - Responde 204")
    void reactivar_Responde204() throws Exception {
        mockMvc.perform(patch("/grupos/{id}/reactivar", 1))
                .andExpect(status().isNoContent());
    }
}
