package com.fides.sienep.controller;

import com.fides.sienep.dto.FrecuenciaRequestDTO;
import com.fides.sienep.dto.FrecuenciaResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.TipoFrecuencia;
import com.fides.sienep.service.FrecuenciaService;
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
@WebMvcTest(FrecuenciaController.class)
class FrecuenciaControllerTest extends AbstractControllerTest {

    @MockitoBean private FrecuenciaService frecuenciaService;

    private FrecuenciaRequestDTO requestValido() {
        FrecuenciaRequestDTO dto = new FrecuenciaRequestDTO();
        dto.setDescripcion("Cada 15 días");
        dto.setTipo(TipoFrecuencia.PERSONALIZADA);
        dto.setIntDias(15);
        return dto;
    }

    private FrecuenciaResponseDTO responseDTO() {
        return FrecuenciaResponseDTO.builder()
                .idFrecuencia(1)
                .descripcion("Cada 15 días")
                .tipo(TipoFrecuencia.PERSONALIZADA)
                .estado(EstadoLogico.ACTIVO)
                .build();
    }

    @Test
    @DisplayName("Crear - Datos válidos responde 201")
    void crear_Valido_Responde201() throws Exception {
        when(frecuenciaService.crear(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/frecuencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idFrecuencia").value(1));
    }

    @Test
    @DisplayName("Crear - Descripción en blanco responde 400 (Bean Validation)")
    void crear_DescripcionEnBlanco_Responde400() throws Exception {
        FrecuenciaRequestDTO dto = requestValido();
        dto.setDescripcion("   ");

        mockMvc.perform(post("/frecuencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Sin tipo responde 400 (Bean Validation)")
    void crear_SinTipo_Responde400() throws Exception {
        FrecuenciaRequestDTO dto = requestValido();
        dto.setTipo(null);

        mockMvc.perform(post("/frecuencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Obtener por ID - Existente responde 200")
    void obtenerPorId_Existente_Responde200() throws Exception {
        when(frecuenciaService.obtenerPorId(1)).thenReturn(responseDTO());

        mockMvc.perform(get("/frecuencias/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idFrecuencia").value(1));
    }

    @Test
    @DisplayName("Obtener por ID - Inexistente responde 404")
    void obtenerPorId_Inexistente_Responde404() throws Exception {
        when(frecuenciaService.obtenerPorId(99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Frecuencia no encontrada"));

        mockMvc.perform(get("/frecuencias/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Listar - Responde 200 con la lista del service")
    void listar_Responde200() throws Exception {
        when(frecuenciaService.listar(eq(EstadoLogico.ACTIVO))).thenReturn(List.of(responseDTO()));

        mockMvc.perform(get("/frecuencias").param("estado", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idFrecuencia").value(1));
    }

    @Test
    @DisplayName("Desactivar - En uso por recordatorios activos responde 409 (propagado desde el service)")
    void desactivar_EnUso_Responde409() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "La frecuencia está en uso por recordatorios activos"))
                .when(frecuenciaService).desactivar(1);

        mockMvc.perform(delete("/frecuencias/{id}", 1))
                .andExpect(status().isConflict());
    }
}
