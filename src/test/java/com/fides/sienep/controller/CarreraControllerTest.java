package com.fides.sienep.controller;

import com.fides.sienep.dto.CarreraRequestDTO;
import com.fides.sienep.dto.CarreraResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.CarreraService;
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

// P1 de la auditoría (sección 5): fija el contrato HTTP del controller (@Valid →
// 400, 404 en recurso inexistente, forma del JSON) — es justo lo que el frontend
// va a consumir. A diferencia de los tests de los controllers de comentarios
// confidenciales (P0), acá el service SÍ se mockea (@WebMvcTest clásico): el
// objetivo de este test no es la autorización (@PreAuthorize vive en el service
// y no se ejerce con el service mockeado), sino el contrato de entrada/salida.
// Ver AbstractControllerTest para el porqué de los mocks de infraestructura.
@WebMvcTest(CarreraController.class)
class CarreraControllerTest extends AbstractControllerTest {

    @MockitoBean private CarreraService carreraService;

    private CarreraResponseDTO responseDTO() {
        return CarreraResponseDTO.builder()
                .idCarrera(1)
                .nomCarrera("Ingeniería en Sistemas")
                .estado(EstadoLogico.ACTIVO)
                .build();
    }

    @Test
    @DisplayName("Crear - Nombre válido responde 201 con el DTO creado")
    void crear_Valido_Responde201() throws Exception {
        CarreraRequestDTO dto = new CarreraRequestDTO();
        dto.setNomCarrera("Ingeniería en Sistemas");
        when(carreraService.crear(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/carreras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCarrera").value(1))
                .andExpect(jsonPath("$.nomCarrera").value("Ingeniería en Sistemas"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    @DisplayName("Crear - Nombre en blanco responde 400 (Bean Validation)")
    void crear_NombreEnBlanco_Responde400() throws Exception {
        CarreraRequestDTO dto = new CarreraRequestDTO();
        dto.setNomCarrera("   ");

        mockMvc.perform(post("/carreras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Nombre de más de 80 caracteres responde 400 (Bean Validation)")
    void crear_NombreDemasiadoLargo_Responde400() throws Exception {
        CarreraRequestDTO dto = new CarreraRequestDTO();
        dto.setNomCarrera("A".repeat(81));

        mockMvc.perform(post("/carreras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Nombre duplicado responde 409 (propagado desde el service)")
    void crear_NombreDuplicado_Responde409() throws Exception {
        CarreraRequestDTO dto = new CarreraRequestDTO();
        dto.setNomCarrera("Ingeniería en Sistemas");
        when(carreraService.crear(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una carrera con ese nombre."));

        mockMvc.perform(post("/carreras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Obtener por ID - Existente responde 200")
    void obtenerPorId_Existente_Responde200() throws Exception {
        when(carreraService.obtenerPorId(1)).thenReturn(responseDTO());

        mockMvc.perform(get("/carreras/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCarrera").value(1));
    }

    @Test
    @DisplayName("Obtener por ID - Inexistente responde 404")
    void obtenerPorId_Inexistente_Responde404() throws Exception {
        when(carreraService.obtenerPorId(99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrera no encontrada"));

        mockMvc.perform(get("/carreras/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Listar - Responde 200 con la lista del service")
    void listar_Responde200() throws Exception {
        when(carreraService.listar(eq(EstadoLogico.ACTIVO))).thenReturn(List.of(responseDTO()));

        mockMvc.perform(get("/carreras").param("estado", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idCarrera").value(1));
    }
}
