package com.fides.sienep.controller;

import com.fides.sienep.dto.InstanciaComunRequestDTO;
import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.dto.InstanciaComunUpdateDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.InstanciaComunService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// P1 de la auditoría — mismo criterio que CarreraControllerTest (ver ese archivo
// y AbstractControllerTest para el porqué del diseño).
@WebMvcTest(InstanciaComunController.class)
class InstanciaComunControllerTest extends AbstractControllerTest {

    @MockitoBean private InstanciaComunService instanciaComunService;

    private InstanciaComunRequestDTO requestValido() {
        InstanciaComunRequestDTO dto = new InstanciaComunRequestDTO();
        dto.setTitulo("Reunión de seguimiento");
        dto.setFechaHora(LocalDateTime.now().plusDays(1));
        dto.setIdEstudiante(5);
        dto.setIdCategoria(1);
        return dto;
    }

    private InstanciaComunResponseDTO responseDTO() {
        InstanciaComunResponseDTO dto = new InstanciaComunResponseDTO();
        dto.setCodInstancia(1);
        dto.setIdNegInstancia("INST-2026-000001");
        dto.setTitulo("Reunión de seguimiento");
        dto.setEstado(EstadoLogico.ACTIVO);
        return dto;
    }

    @Test
    @DisplayName("Crear - Datos válidos responde 201")
    void crear_Valido_Responde201() throws Exception {
        when(instanciaComunService.crear(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codInstancia").value(1))
                .andExpect(jsonPath("$.idNegInstancia").value("INST-2026-000001"));
    }

    @Test
    @DisplayName("Crear - Título en blanco responde 400 (Bean Validation)")
    void crear_TituloEnBlanco_Responde400() throws Exception {
        InstanciaComunRequestDTO dto = requestValido();
        dto.setTitulo("   ");

        mockMvc.perform(post("/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Sin idEstudiante responde 400 (Bean Validation)")
    void crear_SinIdEstudiante_Responde400() throws Exception {
        InstanciaComunRequestDTO dto = requestValido();
        dto.setIdEstudiante(null);

        mockMvc.perform(post("/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Estudiante no encontrado responde 404 (propagado desde el service)")
    void crear_EstudianteNoEncontrado_Responde404() throws Exception {
        when(instanciaComunService.crear(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        mockMvc.perform(post("/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Crear - Estudiante no activo responde 422 (propagado desde el service)")
    void crear_EstudianteNoActivo_Responde422() throws Exception {
        when(instanciaComunService.crear(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "El estudiante no está activo"));

        mockMvc.perform(post("/instancias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Obtener por ID - Existente responde 200")
    void obtenerPorId_Existente_Responde200() throws Exception {
        when(instanciaComunService.obtenerPorId(1)).thenReturn(responseDTO());

        mockMvc.perform(get("/instancias/{codInstancia}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codInstancia").value(1));
    }

    @Test
    @DisplayName("Obtener por ID - Inexistente responde 404")
    void obtenerPorId_Inexistente_Responde404() throws Exception {
        when(instanciaComunService.obtenerPorId(99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Instancia no encontrada"));

        mockMvc.perform(get("/instancias/{codInstancia}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Listar - Sin filtros responde 200 con la página del service")
    void listar_SinFiltros_Responde200() throws Exception {
        Page<InstanciaComunResponseDTO> pagina = new PageImpl<>(List.of(responseDTO()), PageRequest.of(0, 20), 1);
        when(instanciaComunService.listar(any(), any(), any(), any(), any())).thenReturn(pagina);

        mockMvc.perform(get("/instancias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codInstancia").value(1));
    }

    @Test
    @DisplayName("Listar - Con varios filtros se los pasa todos al service, no solo el primero")
    void listar_ConVariosFiltros_SePasanTodosAlService() throws Exception {
        Page<InstanciaComunResponseDTO> pagina = new PageImpl<>(List.of(responseDTO()), PageRequest.of(0, 20), 1);
        when(instanciaComunService.listar(eq(5), isNull(), eq(2), eq(EstadoLogico.INACTIVO), any())).thenReturn(pagina);

        mockMvc.perform(get("/instancias")
                        .param("idEstudiante", "5")
                        .param("idCategoria", "2")
                        .param("estado", "INACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codInstancia").value(1));
    }

    @Test
    @DisplayName("Editar - Datos válidos responde 200")
    void editar_Valido_Responde200() throws Exception {
        InstanciaComunUpdateDTO dto = new InstanciaComunUpdateDTO();
        dto.setTitulo("Reunión reprogramada");
        dto.setFechaHora(LocalDateTime.now().plusDays(2));
        dto.setIdCategoria(1);
        when(instanciaComunService.editar(eq(1), any())).thenReturn(responseDTO());

        mockMvc.perform(put("/instancias/{codInstancia}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codInstancia").value(1));
    }

    @Test
    @DisplayName("Desactivar - Existente responde 204")
    void desactivar_Existente_Responde204() throws Exception {
        mockMvc.perform(delete("/instancias/{codInstancia}", 1))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Desactivar - Inexistente responde 404")
    void desactivar_Inexistente_Responde404() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Instancia no encontrada"))
                .when(instanciaComunService).desactivar(99);

        mockMvc.perform(delete("/instancias/{codInstancia}", 99))
                .andExpect(status().isNotFound());
    }
}
