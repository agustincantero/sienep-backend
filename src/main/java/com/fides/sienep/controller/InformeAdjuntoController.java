package com.fides.sienep.controller;

import com.fides.sienep.dto.ArchivoDescargaDTO;
import com.fides.sienep.dto.InformeAdjuntoRequestDTO;
import com.fides.sienep.dto.InformeAdjuntoResponseDTO;
import com.fides.sienep.service.InformeAdjuntoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/informes-adjuntos")
@RequiredArgsConstructor
@Tag(name = "Informes Adjuntos", description = "Gestión de informes adjuntos de estudiantes")
public class InformeAdjuntoController {

    private final InformeAdjuntoService informeService;

    @GetMapping
    @Operation(summary = "Listar informes de un estudiante")
    @ApiResponse(responseCode = "200", description = "Lista de informes")
    public ResponseEntity<List<InformeAdjuntoResponseDTO>> listar(
            @RequestParam Integer idUsuario) {
        return ResponseEntity.ok(informeService.listarPorEstudiante(idUsuario));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener informe por ID")
    @ApiResponse(responseCode = "200", description = "Informe encontrado")
    @ApiResponse(responseCode = "404", description = "Informe no encontrado")
    public ResponseEntity<InformeAdjuntoResponseDTO> obtenerPorId(
            @PathVariable Integer id) {
        return ResponseEntity.ok(informeService.obtenerPorId(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Adjuntar informe a un estudiante")
    @ApiResponse(responseCode = "201", description = "Informe adjuntado")
    @ApiResponse(responseCode = "400", description = "Extensión no permitida, archivo vacío o demasiado grande")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    @ApiResponse(responseCode = "409", description = "Ya existe un informe con ese nombre para este estudiante")
    public ResponseEntity<InformeAdjuntoResponseDTO> adjuntar(
            @Valid @ModelAttribute InformeAdjuntoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(informeService.adjuntar(dto));
    }

    @GetMapping("/{id}/descargar")
    @Operation(summary = "Descargar el archivo de un informe adjunto")
    @ApiResponse(responseCode = "200", description = "Archivo descargado")
    @ApiResponse(responseCode = "404", description = "Informe no encontrado")
    public ResponseEntity<Resource> descargar(@PathVariable Integer id) {
        ArchivoDescargaDTO archivo = informeService.descargar(id);
        return ResponseEntity.ok()
                .contentType(archivo.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + archivo.nombreDescarga() + "\"")
                .body(archivo.recurso());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Baja lógica de informe adjunto")
    @ApiResponse(responseCode = "204", description = "Informe eliminado")
    @ApiResponse(responseCode = "404", description = "Informe no encontrado")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        informeService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}