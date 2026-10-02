package com.fides.sienep.controller;

import com.fides.sienep.dto.ArchivoDescargaDTO;
import com.fides.sienep.dto.InformeAdjuntoResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.InformeAdjuntoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// P1 de la auditoría — mismo criterio que CarreraControllerTest (ver ese archivo
// y AbstractControllerTest para el porqué del diseño).
@WebMvcTest(InformeAdjuntoController.class)
class InformeAdjuntoControllerTest extends AbstractControllerTest {

    @MockitoBean private InformeAdjuntoService informeService;

    private InformeAdjuntoResponseDTO responseDTO() {
        return InformeAdjuntoResponseDTO.builder()
                .idInforme(1)
                .idUsuario(5)
                .nombre("informe.pdf")
                .estado(EstadoLogico.ACTIVO)
                .build();
    }

    @Test
    @DisplayName("Adjuntar - Datos válidos responde 201")
    void adjuntar_Valido_Responde201() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "informe.pdf", MediaType.APPLICATION_PDF_VALUE, "contenido".getBytes());
        when(informeService.adjuntar(any())).thenReturn(responseDTO());

        mockMvc.perform(multipart("/informes-adjuntos")
                        .file(archivo)
                        .param("idUsuario", "5")
                        .param("nombre", "informe_psicopedagogico"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idInforme").value(1));
    }

    @Test
    @DisplayName("Adjuntar - Sin archivo responde 400 (Bean Validation)")
    void adjuntar_SinArchivo_Responde400() throws Exception {
        mockMvc.perform(multipart("/informes-adjuntos")
                        .param("idUsuario", "5")
                        .param("nombre", "informe_psicopedagogico"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Adjuntar - Estudiante no encontrado responde 404 (propagado desde el service)")
    void adjuntar_EstudianteNoEncontrado_Responde404() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "informe.pdf", MediaType.APPLICATION_PDF_VALUE, "contenido".getBytes());
        when(informeService.adjuntar(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        mockMvc.perform(multipart("/informes-adjuntos")
                        .file(archivo)
                        .param("idUsuario", "99")
                        .param("nombre", "informe_psicopedagogico"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Obtener por ID - Existente responde 200")
    void obtenerPorId_Existente_Responde200() throws Exception {
        when(informeService.obtenerPorId(1)).thenReturn(responseDTO());

        mockMvc.perform(get("/informes-adjuntos/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idInforme").value(1));
    }

    @Test
    @DisplayName("Obtener por ID - Inexistente responde 404")
    void obtenerPorId_Inexistente_Responde404() throws Exception {
        when(informeService.obtenerPorId(99))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Informe no encontrado"));

        mockMvc.perform(get("/informes-adjuntos/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Listar - Responde 200 con la página del service")
    void listar_Responde200() throws Exception {
        Page<InformeAdjuntoResponseDTO> pagina = new PageImpl<>(List.of(responseDTO()), PageRequest.of(0, 20), 1);
        when(informeService.listarPorEstudiante(any(), any())).thenReturn(pagina);

        mockMvc.perform(get("/informes-adjuntos").param("idUsuario", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idInforme").value(1));
    }

    @Test
    @DisplayName("Descargar - Existente responde 200 con el archivo")
    void descargar_Existente_Responde200() throws Exception {
        ArchivoDescargaDTO archivo = new ArchivoDescargaDTO(
                new ByteArrayResource("contenido".getBytes()), MediaType.APPLICATION_PDF, "informe.pdf");
        when(informeService.descargar(1)).thenReturn(archivo);

        mockMvc.perform(get("/informes-adjuntos/{id}/descargar", 1))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"informe.pdf\""));
    }

    @Test
    @DisplayName("Eliminar - Responde 204")
    void eliminar_Responde204() throws Exception {
        mockMvc.perform(delete("/informes-adjuntos/{id}", 1))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Mis informes - GET /informes-adjuntos/mios va a listarMios, no a obtenerPorId")
    void listarMios_RutaPropia() throws Exception {
        when(informeService.listarMios(any())).thenReturn(new PageImpl<>(List.of(responseDTO())));

        mockMvc.perform(get("/informes-adjuntos/mios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idInforme").value(1));

        verify(informeService).listarMios(any());
        verify(informeService, never()).obtenerPorId(any());
    }
}
