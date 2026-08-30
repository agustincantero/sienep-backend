package com.fides.sienep.controller;

import com.fides.sienep.dto.ComentarioNormalInstanciaRequestDTO;
import com.fides.sienep.dto.ComentarioNormalInstanciaResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.ComentarioNormalInstanciaService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// P1 de la auditoría — mismo criterio que CarreraControllerTest (ver ese archivo
// y AbstractControllerTest para el porqué del diseño). No confundir con los
// tests de comentarios CONFIDENCIALES (P0): acá el service se mockea.
@WebMvcTest(ComentarioNormalInstanciaController.class)
class ComentarioNormalInstanciaControllerTest extends AbstractControllerTest {

    @MockitoBean private ComentarioNormalInstanciaService comentarioService;

    private ComentarioNormalInstanciaResponseDTO responseDTO() {
        ComentarioNormalInstanciaResponseDTO dto = new ComentarioNormalInstanciaResponseDTO();
        dto.setIdComentario(1);
        dto.setCodInstancia(1);
        dto.setContenido("Comentario de prueba");
        dto.setEstado(EstadoLogico.ACTIVO);
        return dto;
    }

    @Test
    @DisplayName("Crear - Datos válidos responde 201")
    void crear_Valido_Responde201() throws Exception {
        ComentarioNormalInstanciaRequestDTO dto = new ComentarioNormalInstanciaRequestDTO();
        dto.setContenido("Comentario de prueba");
        when(comentarioService.crear(eq(1), any())).thenReturn(responseDTO());

        mockMvc.perform(post("/instancias/{codInstancia}/comentarios-normales", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idComentario").value(1));
    }

    @Test
    @DisplayName("Crear - Contenido en blanco responde 400 (Bean Validation)")
    void crear_ContenidoEnBlanco_Responde400() throws Exception {
        ComentarioNormalInstanciaRequestDTO dto = new ComentarioNormalInstanciaRequestDTO();
        dto.setContenido("   ");

        mockMvc.perform(post("/instancias/{codInstancia}/comentarios-normales", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Instancia no encontrada responde 404 (propagado desde el service)")
    void crear_InstanciaNoEncontrada_Responde404() throws Exception {
        ComentarioNormalInstanciaRequestDTO dto = new ComentarioNormalInstanciaRequestDTO();
        dto.setContenido("Comentario de prueba");
        when(comentarioService.crear(eq(99), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró una instancia con el id 99"));

        mockMvc.perform(post("/instancias/{codInstancia}/comentarios-normales", 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Listar - Responde 200 con la página del service")
    void listar_Responde200() throws Exception {
        Page<ComentarioNormalInstanciaResponseDTO> pagina = new PageImpl<>(List.of(responseDTO()), PageRequest.of(0, 20), 1);
        when(comentarioService.listarPorInstancia(eq(1), any())).thenReturn(pagina);

        mockMvc.perform(get("/instancias/{codInstancia}/comentarios-normales", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idComentario").value(1));
    }

    @Test
    @DisplayName("Obtener por ID - Existente responde 200")
    void obtenerPorId_Existente_Responde200() throws Exception {
        when(comentarioService.obtenerPorId(1, 1)).thenReturn(responseDTO());

        mockMvc.perform(get("/instancias/{codInstancia}/comentarios-normales/{idComentario}", 1, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idComentario").value(1));
    }

    @Test
    @DisplayName("Obtener por ID - Pertenece a otra instancia responde 404")
    void obtenerPorId_PerteneceAOtraInstancia_Responde404() throws Exception {
        when(comentarioService.obtenerPorId(2, 1))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el comentario 1 en la instancia 2"));

        mockMvc.perform(get("/instancias/{codInstancia}/comentarios-normales/{idComentario}", 2, 1))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Editar - Datos válidos responde 200")
    void editar_Valido_Responde200() throws Exception {
        ComentarioNormalInstanciaRequestDTO dto = new ComentarioNormalInstanciaRequestDTO();
        dto.setContenido("Comentario editado");
        when(comentarioService.editar(eq(1), eq(1), any())).thenReturn(responseDTO());

        mockMvc.perform(put("/instancias/{codInstancia}/comentarios-normales/{idComentario}", 1, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idComentario").value(1));
    }

    @Test
    @DisplayName("Eliminar - Responde 204")
    void eliminar_Responde204() throws Exception {
        mockMvc.perform(delete("/instancias/{codInstancia}/comentarios-normales/{idComentario}", 1, 1))
                .andExpect(status().isNoContent());
    }
}
