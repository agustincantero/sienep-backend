package com.fides.sienep.controller;

import com.fides.sienep.dto.FuncionarioRequestDTO;
import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.dto.FuncionarioUpdateDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.service.FuncionarioService;
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
// y AbstractControllerTest para el porqué del diseño).
@WebMvcTest(FuncionarioController.class)
class FuncionarioControllerTest extends AbstractControllerTest {

    @MockitoBean private FuncionarioService funcionarioService;

    private FuncionarioRequestDTO requestValido() {
        FuncionarioRequestDTO dto = new FuncionarioRequestDTO();
        dto.setNombre("Juan");
        dto.setApellido("García");
        dto.setDocumento("87654321");
        dto.setEmail("juan.garcia@utec.edu.uy");
        dto.setIdRol(2);
        dto.setPaisDocumento(PaisDocumento.URY);
        return dto;
    }

    private FuncionarioResponseDTO responseDTO() {
        return FuncionarioResponseDTO.builder()
                .idUsuario(2)
                .nombre("Juan")
                .apellido("García")
                .email("juan.garcia@utec.edu.uy")
                .estado(EstadoLogico.PENDIENTE_DE_ACTIVACION)
                .build();
    }

    @Test
    @DisplayName("Crear - Datos válidos responde 201")
    void crear_Valido_Responde201() throws Exception {
        when(funcionarioService.crear(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/funcionarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(2))
                .andExpect(jsonPath("$.email").value("juan.garcia@utec.edu.uy"));
    }

    @Test
    @DisplayName("Crear - Email inválido responde 400 (Bean Validation)")
    void crear_EmailInvalido_Responde400() throws Exception {
        FuncionarioRequestDTO dto = requestValido();
        dto.setEmail("no-es-un-email");

        mockMvc.perform(post("/funcionarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Sin idRol responde 400 (Bean Validation)")
    void crear_SinIdRol_Responde400() throws Exception {
        FuncionarioRequestDTO dto = requestValido();
        dto.setIdRol(null);

        mockMvc.perform(post("/funcionarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Documento duplicado responde 409 (propagado desde el service)")
    void crear_DocumentoDuplicado_Responde409() throws Exception {
        when(funcionarioService.crear(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un funcionario con ese documento."));

        mockMvc.perform(post("/funcionarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Obtener por ID - Existente responde 200")
    void obtenerPorId_Existente_Responde200() throws Exception {
        when(funcionarioService.obtenerPorId(2)).thenReturn(responseDTO());

        mockMvc.perform(get("/funcionarios/{id}", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(2));
    }

    @Test
    @DisplayName("Obtener por ID - Inexistente responde 404")
    void obtenerPorId_Inexistente_Responde404() throws Exception {
        when(funcionarioService.obtenerPorId(99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionario no encontrado"));

        mockMvc.perform(get("/funcionarios/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Editar - Datos válidos responde 200")
    void editar_Valido_Responde200() throws Exception {
        FuncionarioUpdateDTO dto = new FuncionarioUpdateDTO();
        dto.setNombre("Juan");
        dto.setApellido("García");
        dto.setEmail("juan.garcia@utec.edu.uy");
        dto.setIdRol(2);
        when(funcionarioService.editar(eq(2), any())).thenReturn(responseDTO());

        mockMvc.perform(put("/funcionarios/{id}", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(2));
    }

    @Test
    @DisplayName("Listar - Responde 200 con la página del service")
    void listar_Responde200() throws Exception {
        Page<FuncionarioResponseDTO> pagina = new PageImpl<>(List.of(responseDTO()), PageRequest.of(0, 20), 1);
        when(funcionarioService.listar(any(), any(), any())).thenReturn(pagina);

        mockMvc.perform(get("/funcionarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idUsuario").value(2));
    }

    @Test
    @DisplayName("Desactivar - Responde 204")
    void desactivar_Responde204() throws Exception {
        mockMvc.perform(delete("/funcionarios/{id}", 2))
                .andExpect(status().isNoContent());
    }
}
