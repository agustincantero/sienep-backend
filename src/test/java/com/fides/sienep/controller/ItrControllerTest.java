package com.fides.sienep.controller;

import com.fides.sienep.dto.ItrRequestDTO;
import com.fides.sienep.dto.ItrResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.ItrService;
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
@WebMvcTest(ItrController.class)
class ItrControllerTest extends AbstractControllerTest {

    @MockitoBean private ItrService itrService;

    private ItrRequestDTO requestValido() {
        ItrRequestDTO dto = new ItrRequestDTO();
        dto.setNomItr("ITR Centro-Sur");
        dto.setCarrerasIds(List.of());
        return dto;
    }

    private ItrResponseDTO responseDTO() {
        return ItrResponseDTO.builder()
                .idItr(1)
                .nomItr("ITR Centro-Sur")
                .estado(EstadoLogico.ACTIVO)
                .build();
    }

    @Test
    @DisplayName("Crear - Datos válidos responde 201")
    void crear_Valido_Responde201() throws Exception {
        when(itrService.crear(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/itrs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idItr").value(1))
                .andExpect(jsonPath("$.nomItr").value("ITR Centro-Sur"));
    }

    @Test
    @DisplayName("Crear - Nombre en blanco responde 400 (Bean Validation)")
    void crear_NombreEnBlanco_Responde400() throws Exception {
        ItrRequestDTO dto = requestValido();
        dto.setNomItr("   ");

        mockMvc.perform(post("/itrs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Sin lista de carreras responde 400 (@NotNull)")
    void crear_SinListaDeCarreras_Responde400() throws Exception {
        ItrRequestDTO dto = requestValido();
        dto.setCarrerasIds(null);

        mockMvc.perform(post("/itrs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Nombre duplicado responde 409 (propagado desde el service)")
    void crear_NombreDuplicado_Responde409() throws Exception {
        when(itrService.crear(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un ITR con ese nombre."));

        mockMvc.perform(post("/itrs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Obtener por ID - Existente responde 200")
    void obtenerPorId_Existente_Responde200() throws Exception {
        when(itrService.obtenerPorId(1)).thenReturn(responseDTO());

        mockMvc.perform(get("/itrs/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idItr").value(1));
    }

    @Test
    @DisplayName("Obtener por ID - Inexistente responde 404")
    void obtenerPorId_Inexistente_Responde404() throws Exception {
        when(itrService.obtenerPorId(99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "ITR no encontrado"));

        mockMvc.perform(get("/itrs/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Editar - ITR inactivo responde 409 (propagado desde el service)")
    void editar_ItrInactivo_Responde409() throws Exception {
        when(itrService.editar(eq(1), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "No se puede modificar ni asociar carreras a un ITR inactivo."));

        mockMvc.perform(put("/itrs/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Listar - Responde 200 con la lista del service")
    void listar_Responde200() throws Exception {
        when(itrService.listar(eq(EstadoLogico.ACTIVO))).thenReturn(List.of(responseDTO()));

        mockMvc.perform(get("/itrs").param("estado", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idItr").value(1));
    }

    @Test
    @DisplayName("Desactivar - Responde 204")
    void desactivar_Responde204() throws Exception {
        mockMvc.perform(delete("/itrs/{id}", 1))
                .andExpect(status().isNoContent());
    }
}
