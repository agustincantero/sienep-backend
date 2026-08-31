package com.fides.sienep.controller;

import com.fides.sienep.dto.RolRequestDTO;
import com.fides.sienep.dto.RolResponseDTO;
import com.fides.sienep.dto.RolUpdateDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.RolService;
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
@WebMvcTest(RolController.class)
class RolControllerTest extends AbstractControllerTest {

    @MockitoBean private RolService rolService;

    private RolResponseDTO responseDTO() {
        RolResponseDTO dto = new RolResponseDTO();
        dto.setIdRol(1);
        dto.setNombre("TUTOR");
        dto.setEstado(EstadoLogico.ACTIVO);
        return dto;
    }

    @Test
    @DisplayName("Crear - Datos válidos responde 201")
    void crear_Valido_Responde201() throws Exception {
        RolRequestDTO dto = new RolRequestDTO();
        dto.setNombre("TUTOR");
        when(rolService.crear(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idRol").value(1));
    }

    @Test
    @DisplayName("Crear - Nombre en blanco responde 400 (Bean Validation)")
    void crear_NombreEnBlanco_Responde400() throws Exception {
        RolRequestDTO dto = new RolRequestDTO();
        dto.setNombre("   ");

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Nombre duplicado responde 409 (propagado desde el service)")
    void crear_NombreDuplicado_Responde409() throws Exception {
        RolRequestDTO dto = new RolRequestDTO();
        dto.setNombre("TUTOR");
        when(rolService.crear(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "El nombre del rol ya existe"));

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Obtener por ID - Existente responde 200")
    void obtenerPorId_Existente_Responde200() throws Exception {
        when(rolService.obtenerPorId(1)).thenReturn(responseDTO());

        mockMvc.perform(get("/roles/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idRol").value(1));
    }

    @Test
    @DisplayName("Obtener por ID - Inexistente responde 404")
    void obtenerPorId_Inexistente_Responde404() throws Exception {
        when(rolService.obtenerPorId(99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));

        mockMvc.perform(get("/roles/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Editar - Rol protegido (ADMINISTRADOR/ESTUDIANTE) responde 403 (propagado desde el service)")
    void editar_RolProtegido_Responde403() throws Exception {
        RolUpdateDTO dto = new RolUpdateDTO();
        dto.setNombre("ADMINISTRADOR");
        when(rolService.editar(eq(1), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Acción denegada: No se puede modificar ni desactivar el rol base ADMINISTRADOR."));

        mockMvc.perform(put("/roles/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Listar - Responde 200 con la lista del service")
    void listar_Responde200() throws Exception {
        when(rolService.listar(eq(EstadoLogico.ACTIVO))).thenReturn(List.of(responseDTO()));

        mockMvc.perform(get("/roles").param("estado", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idRol").value(1));
    }

    @Test
    @DisplayName("Desactivar - Responde 204")
    void desactivar_Responde204() throws Exception {
        mockMvc.perform(delete("/roles/{id}", 1))
                .andExpect(status().isNoContent());
    }
}
