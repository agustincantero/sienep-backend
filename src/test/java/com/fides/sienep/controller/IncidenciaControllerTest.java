package com.fides.sienep.controller;

import com.fides.sienep.dto.IncidenciaRequestDTO;
import com.fides.sienep.dto.IncidenciaResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.IncidenciaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// P1 de la auditoría — mismo criterio que CarreraControllerTest (ver ese archivo
// y AbstractControllerTest para el porqué del diseño).
@WebMvcTest(IncidenciaController.class)
class IncidenciaControllerTest extends AbstractControllerTest {

    @MockitoBean private IncidenciaService incidenciaService;

    private IncidenciaRequestDTO requestValido() {
        IncidenciaRequestDTO dto = new IncidenciaRequestDTO();
        dto.setTitulo("Incidente en el aula");
        dto.setFechaHora(LocalDateTime.now().minusHours(1));
        dto.setIdEstudiante(5);
        dto.setLugar("Aula 201");
        return dto;
    }

    private IncidenciaResponseDTO responseDTO() {
        IncidenciaResponseDTO dto = new IncidenciaResponseDTO();
        dto.setCodInstancia(1);
        dto.setTitulo("Incidente en el aula");
        dto.setLugar("Aula 201");
        dto.setEstado(EstadoLogico.ACTIVO);
        return dto;
    }

    @Test
    @DisplayName("Crear - Datos válidos responde 201")
    void crear_Valido_Responde201() throws Exception {
        when(incidenciaService.crear(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/incidencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codInstancia").value(1));
    }

    @Test
    @DisplayName("Crear - Lugar en blanco responde 400 (Bean Validation)")
    void crear_LugarEnBlanco_Responde400() throws Exception {
        IncidenciaRequestDTO dto = requestValido();
        dto.setLugar("   ");

        mockMvc.perform(post("/incidencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Estudiante no encontrado responde 404 (propagado desde el service)")
    void crear_EstudianteNoEncontrado_Responde404() throws Exception {
        when(incidenciaService.crear(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        mockMvc.perform(post("/incidencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Obtener por ID - Existente responde 200")
    void obtenerPorId_Existente_Responde200() throws Exception {
        when(incidenciaService.obtenerPorId(1)).thenReturn(responseDTO());

        mockMvc.perform(get("/incidencias/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codInstancia").value(1));
    }

    @Test
    @DisplayName("Obtener por ID - Inexistente responde 404")
    void obtenerPorId_Inexistente_Responde404() throws Exception {
        when(incidenciaService.obtenerPorId(99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidencia no encontrada"));

        mockMvc.perform(get("/incidencias/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Listar - Sin filtros responde 200 con la página del service")
    void listar_SinFiltros_Responde200() throws Exception {
        Page<IncidenciaResponseDTO> pagina = new PageImpl<>(List.of(responseDTO()), PageRequest.of(0, 20), 1);
        when(incidenciaService.listar(any(), any(), any(), any())).thenReturn(pagina);

        mockMvc.perform(get("/incidencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codInstancia").value(1));
    }

    @Test
    @DisplayName("Listar - Sin sort usa el orden por defecto (fecha y código descendentes) para que la paginación sea estable")
    void listar_SinSort_UsaOrdenPorDefecto() throws Exception {
        Page<IncidenciaResponseDTO> pagina = new PageImpl<>(List.of(responseDTO()), PageRequest.of(0, 20), 1);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(incidenciaService.listar(any(), any(), any(), pageable.capture())).thenReturn(pagina);

        mockMvc.perform(get("/incidencias")).andExpect(status().isOk());

        assertEquals(Sort.by(Sort.Direction.DESC, "fechaHora", "codInstancia"), pageable.getValue().getSort());
        assertEquals(20, pageable.getValue().getPageSize());
    }

    @Test
    @DisplayName("Listar - Un sort explícito pisa el orden por defecto")
    void listar_ConSort_PisaElOrdenPorDefecto() throws Exception {
        Page<IncidenciaResponseDTO> pagina = new PageImpl<>(List.of(responseDTO()), PageRequest.of(0, 20), 1);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(incidenciaService.listar(any(), any(), any(), pageable.capture())).thenReturn(pagina);

        mockMvc.perform(get("/incidencias").param("sort", "titulo,asc")).andExpect(status().isOk());

        assertEquals(Sort.by(Sort.Direction.ASC, "titulo"), pageable.getValue().getSort());
    }

    @Test
    @DisplayName("Listar - Con varios filtros se los pasa todos al service, no solo el primero")
    void listar_ConVariosFiltros_SePasanTodosAlService() throws Exception {
        Page<IncidenciaResponseDTO> pagina = new PageImpl<>(List.of(responseDTO()), PageRequest.of(0, 20), 1);
        when(incidenciaService.listar(eq(5), eq(2), eq(EstadoLogico.INACTIVO), any())).thenReturn(pagina);

        mockMvc.perform(get("/incidencias")
                        .param("idEstudiante", "5")
                        .param("idFuncionario", "2")
                        .param("estado", "INACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codInstancia").value(1));
    }

    @Test
    @DisplayName("Desactivar - Responde 204")
    void desactivar_Responde204() throws Exception {
        mockMvc.perform(delete("/incidencias/{id}", 1))
                .andExpect(status().isNoContent());
    }
}
