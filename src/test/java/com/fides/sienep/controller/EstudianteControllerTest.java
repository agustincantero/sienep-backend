package com.fides.sienep.controller;

import com.fides.sienep.dto.ProcesosAbiertosDTO;
import com.fides.sienep.dto.EstudianteRequestDTO;
import com.fides.sienep.dto.EstudianteResponseDTO;
import com.fides.sienep.dto.EstudianteResumenDTO;
import com.fides.sienep.dto.EstudianteUpdateDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.service.EstudianteService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// P1 de la auditoría — mismo criterio que CarreraControllerTest (ver ese archivo
// y AbstractControllerTest para el porqué del diseño).
@WebMvcTest(EstudianteController.class)
class EstudianteControllerTest extends AbstractControllerTest {

    @MockitoBean private EstudianteService estudianteService;

    private EstudianteRequestDTO requestValido() {
        EstudianteRequestDTO dto = new EstudianteRequestDTO();
        dto.setNombre("María");
        dto.setApellido("González");
        dto.setDocumento("12345678");
        dto.setPaisDocumento(PaisDocumento.URY);
        dto.setEmail("maria.gonzalez@utec.edu.uy");
        dto.setFechaNacimiento(LocalDate.of(2000, 5, 15));
        return dto;
    }

    private EstudianteResponseDTO responseDTO() {
        return EstudianteResponseDTO.builder()
                .idUsuario(5)
                .nombre("María")
                .apellido("González")
                .email("maria.gonzalez@utec.edu.uy")
                .estado(EstadoLogico.PENDIENTE_DE_ACTIVACION)
                .build();
    }

    @Test
    @DisplayName("Crear - Datos válidos responde 201")
    void crear_Valido_Responde201() throws Exception {
        when(estudianteService.crear(any())).thenReturn(responseDTO());

        mockMvc.perform(post("/estudiantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(5))
                .andExpect(jsonPath("$.email").value("maria.gonzalez@utec.edu.uy"));
    }

    @Test
    @DisplayName("Crear - Email inválido responde 400 (Bean Validation)")
    void crear_EmailInvalido_Responde400() throws Exception {
        EstudianteRequestDTO dto = requestValido();
        dto.setEmail("no-es-un-email");

        mockMvc.perform(post("/estudiantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Fecha de nacimiento futura responde 400 (@Past)")
    void crear_FechaNacimientoFutura_Responde400() throws Exception {
        EstudianteRequestDTO dto = requestValido();
        dto.setFechaNacimiento(LocalDate.now().plusDays(1));

        mockMvc.perform(post("/estudiantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Documento con caracteres inválidos responde 400 (@Pattern)")
    void crear_DocumentoConCaracteresInvalidos_Responde400() throws Exception {
        EstudianteRequestDTO dto = requestValido();
        dto.setDocumento("12.345-678");

        mockMvc.perform(post("/estudiantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crear - Documento duplicado responde 409 (propagado desde el service)")
    void crear_DocumentoDuplicado_Responde409() throws Exception {
        when(estudianteService.crear(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un estudiante con ese documento."));

        mockMvc.perform(post("/estudiantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Obtener por ID - Existente responde 200")
    void obtenerPorId_Existente_Responde200() throws Exception {
        when(estudianteService.obtenerPorId(5)).thenReturn(responseDTO());

        mockMvc.perform(get("/estudiantes/{id}", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(5));
    }

    @Test
    @DisplayName("Obtener por ID - Inexistente responde 404")
    void obtenerPorId_Inexistente_Responde404() throws Exception {
        when(estudianteService.obtenerPorId(99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado: 99"));

        mockMvc.perform(get("/estudiantes/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Editar - Datos válidos responde 200")
    void editar_Valido_Responde200() throws Exception {
        EstudianteUpdateDTO dto = new EstudianteUpdateDTO();
        dto.setNombre("María");
        dto.setApellido("González");
        dto.setEmail("maria.gonzalez@utec.edu.uy");
        dto.setFechaNacimiento(LocalDate.of(2000, 5, 15));
        when(estudianteService.editar(org.mockito.ArgumentMatchers.eq(5), any())).thenReturn(responseDTO());

        mockMvc.perform(put("/estudiantes/{id}", 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(5));
    }

    @Test
    @DisplayName("Editar - Nombre en blanco responde 400 (Bean Validation)")
    void editar_NombreEnBlanco_Responde400() throws Exception {
        EstudianteUpdateDTO dto = new EstudianteUpdateDTO();
        dto.setNombre("   ");
        dto.setApellido("González");
        dto.setEmail("maria.gonzalez@utec.edu.uy");
        dto.setFechaNacimiento(LocalDate.of(2000, 5, 15));

        mockMvc.perform(put("/estudiantes/{id}", 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Listar - Responde 200 con la página del service")
    void listar_Responde200() throws Exception {
        EstudianteResumenDTO resumen = EstudianteResumenDTO.builder().idUsuario(5).build();
        Page<EstudianteResumenDTO> pagina = new PageImpl<>(List.of(resumen), PageRequest.of(0, 20), 1);
        when(estudianteService.listar(any(), any(), any(), any(), any(), any())).thenReturn(pagina);

        mockMvc.perform(get("/estudiantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idUsuario").value(5));
    }

    @Test
    @DisplayName("Desactivar - Existente responde 204 (sin confirmar = confirmar=false)")
    void desactivar_Existente_Responde204() throws Exception {
        mockMvc.perform(delete("/estudiantes/{id}", 5))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(estudianteService).desactivar(5, false);
    }

    @Test
    @DisplayName("Desactivar - Inexistente responde 404")
    void desactivar_Inexistente_Responde404() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado: 99"))
                .when(estudianteService).desactivar(99, false);

        mockMvc.perform(delete("/estudiantes/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Desactivar - Con procesos abiertos sin confirmar responde 409 (RF06)")
    void desactivar_ProcesosAbiertos_Responde409() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                        "El estudiante tiene 1 instancia agendada. Confirmá la baja para continuar."))
                .when(estudianteService).desactivar(5, false);

        mockMvc.perform(delete("/estudiantes/{id}", 5))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Desactivar - confirmar=true llega al service")
    void desactivar_Confirmado_PasaConfirmar() throws Exception {
        mockMvc.perform(delete("/estudiantes/{id}", 5).param("confirmar", "true"))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(estudianteService).desactivar(5, true);
    }

    @Test
    @DisplayName("Procesos abiertos - Responde los conteos")
    void procesosAbiertos_Responde200() throws Exception {
        when(estudianteService.procesosAbiertos(5)).thenReturn(new ProcesosAbiertosDTO(2, 1));

        mockMvc.perform(get("/estudiantes/{id}/procesos-abiertos", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instanciasFuturas").value(2))
                .andExpect(jsonPath("$.recordatoriosPendientes").value(1));
    }
}
