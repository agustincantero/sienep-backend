package com.fides.sienep.controller;

import com.fides.sienep.dto.TelefonoRequestDTO;
import com.fides.sienep.dto.TelefonoResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.TelefonoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// P1 de la auditoría — mismo criterio que CarreraControllerTest (ver ese archivo
// y AbstractControllerTest para el porqué del diseño).
@WebMvcTest(TelefonoController.class)
class TelefonoControllerTest extends AbstractControllerTest {

    @MockitoBean private TelefonoService telefonoService;

    private TelefonoResponseDTO responseDTO() {
        TelefonoResponseDTO dto = new TelefonoResponseDTO();
        dto.setIdUsuario(5);
        dto.setNumero("099123456");
        dto.setEstado(EstadoLogico.ACTIVO);
        return dto;
    }

    @Test
    @DisplayName("Agregar - Datos válidos responde 201")
    void agregar_Valido_Responde201() throws Exception {
        TelefonoRequestDTO dto = new TelefonoRequestDTO();
        dto.setIdUsuario(5);
        dto.setNumero("099123456");
        when(telefonoService.agregar(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/telefonos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("099123456"));
    }

    @Test
    @DisplayName("Agregar - Número con formato inválido responde 400 (Bean Validation)")
    void agregar_NumeroInvalido_Responde400() throws Exception {
        TelefonoRequestDTO dto = new TelefonoRequestDTO();
        dto.setIdUsuario(5);
        dto.setNumero("abc123");

        mockMvc.perform(post("/telefonos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Agregar - Estudiante no encontrado responde 404 (propagado desde el service)")
    void agregar_EstudianteNoEncontrado_Responde404() throws Exception {
        TelefonoRequestDTO dto = new TelefonoRequestDTO();
        dto.setIdUsuario(99);
        dto.setNumero("099123456");
        when(telefonoService.agregar(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        mockMvc.perform(post("/telefonos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Listar - Responde 200 con la lista del service")
    void listar_Responde200() throws Exception {
        when(telefonoService.listarPorEstudiante(5)).thenReturn(List.of(responseDTO()));

        mockMvc.perform(get("/telefonos").param("idUsuario", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("099123456"));
    }

    @Test
    @DisplayName("Eliminar - Responde 204")
    void eliminar_Responde204() throws Exception {
        mockMvc.perform(delete("/telefonos").param("idUsuario", "5").param("numero", "099123456"))
                .andExpect(status().isNoContent());
    }
}
