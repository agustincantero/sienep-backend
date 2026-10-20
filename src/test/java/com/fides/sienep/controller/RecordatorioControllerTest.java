package com.fides.sienep.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fides.sienep.dto.RecordatorioRequestDTO;
import com.fides.sienep.dto.RecordatorioResponseDTO;
import com.fides.sienep.model.TipoRecordatorio;
import com.fides.sienep.service.InstanciaComunService;
import com.fides.sienep.service.RecordatorioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// P1 de la auditoría — mismo criterio que CarreraControllerTest (ver ese archivo
// y AbstractControllerTest para el porqué del diseño).
@WebMvcTest(RecordatorioController.class)
class RecordatorioControllerTest extends AbstractControllerTest {

    @MockitoBean private RecordatorioService recordatorioService;
    // El controller también depende de InstanciaComunService (endpoint
    // crearInstanciaDesdeRecordatorio) — sin este mock el contexto no levanta.
    @MockitoBean private InstanciaComunService instanciaComunService;

    private RecordatorioRequestDTO requestValido() {
        RecordatorioRequestDTO dto = new RecordatorioRequestDTO();
        dto.setTitulo("Llamar a la familia");
        dto.setTipo(TipoRecordatorio.LLAMADA);
        dto.setFechaHora(LocalDateTime.now().plusDays(1));
        dto.setCodInstancia(1);
        dto.setIdCategoria(1);
        return dto;
    }

    private RecordatorioResponseDTO responseDTO() {
        RecordatorioResponseDTO dto = new RecordatorioResponseDTO();
        dto.setCodRecordatorio(1);
        dto.setIdNegRecordatorio("REC-2026-000001");
        dto.setTitulo("Llamar a la familia");
        return dto;
    }

    @Test
    @DisplayName("Crear - Datos válidos responde 201")
    void crear_Valido_Responde201() throws Exception {
        when(recordatorioService.crear(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/recordatorios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codRecordatorio").value(1))
                .andExpect(jsonPath("$.idNegRecordatorio").value("REC-2026-000001"));
    }

    @Test
    @DisplayName("Crear - Fecha en el pasado responde 400 (@Future)")
    void crear_FechaEnElPasado_Responde400() throws Exception {
        RecordatorioRequestDTO dto = requestValido();
        dto.setFechaHora(LocalDateTime.now().minusDays(1));

        mockMvc.perform(post("/recordatorios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Un idResponsable en el body se ignora y responde 201 (el responsable sale de la sesión)")
    void crear_ConIdResponsableEnElBody_SeIgnora() throws Exception {
        when(recordatorioService.crear(any())).thenReturn(responseDTO());
        ObjectNode body = objectMapper.valueToTree(requestValido());
        body.put("idResponsable", 99);

        mockMvc.perform(post("/recordatorios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Crear - Instancia no encontrada responde 404 (propagado desde el service)")
    void crear_InstanciaNoEncontrada_Responde404() throws Exception {
        when(recordatorioService.crear(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Instancia no encontrada"));

        mockMvc.perform(post("/recordatorios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Obtener por ID - Existente responde 200")
    void obtenerPorId_Existente_Responde200() throws Exception {
        when(recordatorioService.obtenerPorId(1)).thenReturn(responseDTO());

        mockMvc.perform(get("/recordatorios/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codRecordatorio").value(1));
    }

    @Test
    @DisplayName("Obtener por ID - Inexistente responde 404")
    void obtenerPorId_Inexistente_Responde404() throws Exception {
        when(recordatorioService.obtenerPorId(99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Recordatorio no encontrado"));

        mockMvc.perform(get("/recordatorios/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Listar - Responde 200 con la página del service")
    void listar_Responde200() throws Exception {
        Page<RecordatorioResponseDTO> pagina = new PageImpl<>(List.of(responseDTO()), PageRequest.of(0, 20), 1);
        when(recordatorioService.listar(any(), any())).thenReturn(pagina);

        mockMvc.perform(get("/recordatorios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codRecordatorio").value(1));
    }

    @Test
    @DisplayName("Desactivar - Existente responde 204")
    void desactivar_Existente_Responde204() throws Exception {
        mockMvc.perform(delete("/recordatorios/{id}", 1))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Desactivar - Inexistente responde 404")
    void desactivar_Inexistente_Responde404() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Recordatorio no encontrado"))
                .when(recordatorioService).desactivar(99);

        mockMvc.perform(delete("/recordatorios/{id}", 99))
                .andExpect(status().isNotFound());
    }
}
