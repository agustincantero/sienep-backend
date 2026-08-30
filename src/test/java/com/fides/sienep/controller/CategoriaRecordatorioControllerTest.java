package com.fides.sienep.controller;

import com.fides.sienep.dto.CategoriaRecordatorioRequestDTO;
import com.fides.sienep.dto.CategoriaRecordatorioResponseDTO;
import com.fides.sienep.dto.CategoriaRecordatorioUpdateDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.CategoriaRecordatorioService;
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
@WebMvcTest(CategoriaRecordatorioController.class)
class CategoriaRecordatorioControllerTest extends AbstractControllerTest {

    @MockitoBean private CategoriaRecordatorioService categoriaService;

    private CategoriaRecordatorioResponseDTO responseDTO() {
        CategoriaRecordatorioResponseDTO dto = new CategoriaRecordatorioResponseDTO();
        dto.setIdCategoria(1);
        dto.setNomCategoria("SEGUIMIENTO");
        dto.setEstado(EstadoLogico.ACTIVO);
        return dto;
    }

    @Test
    @DisplayName("Crear - Nombre válido responde 201")
    void crear_Valido_Responde201() throws Exception {
        CategoriaRecordatorioRequestDTO dto = new CategoriaRecordatorioRequestDTO();
        dto.setNomCategoria("SEGUIMIENTO");
        when(categoriaService.crear(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/categorias-recordatorios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCategoria").value(1));
    }

    @Test
    @DisplayName("Crear - Nombre en blanco responde 400 (Bean Validation)")
    void crear_NombreEnBlanco_Responde400() throws Exception {
        CategoriaRecordatorioRequestDTO dto = new CategoriaRecordatorioRequestDTO();
        dto.setNomCategoria("   ");

        mockMvc.perform(post("/categorias-recordatorios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Nombre duplicado responde 409 (propagado desde el service)")
    void crear_NombreDuplicado_Responde409() throws Exception {
        CategoriaRecordatorioRequestDTO dto = new CategoriaRecordatorioRequestDTO();
        dto.setNomCategoria("SEGUIMIENTO");
        when(categoriaService.crear(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de la categoría ya existe"));

        mockMvc.perform(post("/categorias-recordatorios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Obtener por ID - Existente responde 200")
    void obtenerPorId_Existente_Responde200() throws Exception {
        when(categoriaService.obtenerPorId(1)).thenReturn(responseDTO());

        mockMvc.perform(get("/categorias-recordatorios/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCategoria").value(1));
    }

    @Test
    @DisplayName("Obtener por ID - Inexistente responde 404")
    void obtenerPorId_Inexistente_Responde404() throws Exception {
        when(categoriaService.obtenerPorId(99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría de recordatorio no encontrada"));

        mockMvc.perform(get("/categorias-recordatorios/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Editar - Categoría inactiva responde 409 (propagado desde el service)")
    void editar_CategoriaInactiva_Responde409() throws Exception {
        CategoriaRecordatorioUpdateDTO dto = new CategoriaRecordatorioUpdateDTO();
        dto.setNomCategoria("SEGUIMIENTO");
        when(categoriaService.editar(eq(1), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "No se puede editar una categoría inactiva. Por favor, reactívela primero."));

        mockMvc.perform(put("/categorias-recordatorios/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Listar - Responde 200 con la lista del service")
    void listar_Responde200() throws Exception {
        when(categoriaService.listar(any(), eq(EstadoLogico.ACTIVO))).thenReturn(List.of(responseDTO()));

        mockMvc.perform(get("/categorias-recordatorios").param("estado", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idCategoria").value(1));
    }

    @Test
    @DisplayName("Desactivar - Con recordatorios activos asociados responde 409 (propagado desde el service)")
    void desactivar_ConRecordatoriosActivos_Responde409() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede desactivar la categoría porque tiene recordatorios activos asociados"))
                .when(categoriaService).desactivar(1);

        mockMvc.perform(delete("/categorias-recordatorios/{id}", 1))
                .andExpect(status().isConflict());
    }
}
