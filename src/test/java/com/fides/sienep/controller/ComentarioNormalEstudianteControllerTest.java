package com.fides.sienep.controller;

import com.fides.sienep.dto.ComentarioNormalEstudianteRequestDTO;
import com.fides.sienep.dto.ComentarioNormalEstudianteResponseDTO;
import com.fides.sienep.dto.ComentarioNormalEstudianteUpdateDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.ComentarioNormalEstudianteService;
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
// tests de comentarios CONFIDENCIALES (P0): acá el service se mockea, no se
// ejerce @PreAuthorize — el objetivo es el contrato HTTP, no la autorización.
@WebMvcTest(ComentarioNormalEstudianteController.class)
class ComentarioNormalEstudianteControllerTest extends AbstractControllerTest {

    @MockitoBean private ComentarioNormalEstudianteService comentarioService;

    private ComentarioNormalEstudianteResponseDTO responseDTO() {
        return ComentarioNormalEstudianteResponseDTO.builder()
                .id(1)
                .contenido("Comentario de prueba")
                .estado(EstadoLogico.ACTIVO)
                .idEstudiante(5)
                .build();
    }

    @Test
    @DisplayName("Crear - Datos válidos responde 201")
    void crear_Valido_Responde201() throws Exception {
        ComentarioNormalEstudianteRequestDTO dto = new ComentarioNormalEstudianteRequestDTO();
        dto.setContenido("Comentario de prueba");
        dto.setIdEstudiante(5);
        when(comentarioService.crear(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/comentarios-normales-estudiante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Crear - Contenido en blanco responde 400 (Bean Validation)")
    void crear_ContenidoEnBlanco_Responde400() throws Exception {
        ComentarioNormalEstudianteRequestDTO dto = new ComentarioNormalEstudianteRequestDTO();
        dto.setContenido("   ");
        dto.setIdEstudiante(5);

        mockMvc.perform(post("/comentarios-normales-estudiante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Estudiante no encontrado responde 404 (propagado desde el service)")
    void crear_EstudianteNoEncontrado_Responde404() throws Exception {
        ComentarioNormalEstudianteRequestDTO dto = new ComentarioNormalEstudianteRequestDTO();
        dto.setContenido("Comentario de prueba");
        dto.setIdEstudiante(99);
        when(comentarioService.crear(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado: 99"));

        mockMvc.perform(post("/comentarios-normales-estudiante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Obtener por ID - Existente responde 200")
    void obtenerPorId_Existente_Responde200() throws Exception {
        when(comentarioService.obtenerPorId(1)).thenReturn(responseDTO());

        mockMvc.perform(get("/comentarios-normales-estudiante/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Obtener por ID - Inexistente responde 404")
    void obtenerPorId_Inexistente_Responde404() throws Exception {
        when(comentarioService.obtenerPorId(99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario no encontrado: 99"));

        mockMvc.perform(get("/comentarios-normales-estudiante/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Listar - Sin idEstudiante responde 200 (el service resuelve según el usuario autenticado)")
    void listar_Responde200() throws Exception {
        Page<ComentarioNormalEstudianteResponseDTO> pagina = new PageImpl<>(List.of(responseDTO()), PageRequest.of(0, 20), 1);
        when(comentarioService.listarPorEstudiante(any(), any())).thenReturn(pagina);

        mockMvc.perform(get("/comentarios-normales-estudiante").param("idEstudiante", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    @DisplayName("Editar - Datos válidos responde 200")
    void editar_Valido_Responde200() throws Exception {
        ComentarioNormalEstudianteUpdateDTO dto = new ComentarioNormalEstudianteUpdateDTO();
        dto.setContenido("Comentario editado");
        when(comentarioService.editar(eq(1), any())).thenReturn(responseDTO());

        mockMvc.perform(put("/comentarios-normales-estudiante/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Eliminar - Responde 204")
    void eliminar_Responde204() throws Exception {
        mockMvc.perform(delete("/comentarios-normales-estudiante/{id}", 1))
                .andExpect(status().isNoContent());
    }
}
