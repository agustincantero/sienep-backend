package com.fides.sienep.controller;

import com.fides.sienep.dto.ComentarioConfidencialInstanciaRequestDTO;
import com.fides.sienep.dto.ComentarioConfidencialInstanciaResponseDTO;
import com.fides.sienep.service.ComentarioConfidencialInstanciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instancias/{codInstancia}/comentarios-confidenciales")
@RequiredArgsConstructor
@Tag(name = "Comentarios Confidenciales de Instancia", description = "Gestión de comentarios confidenciales de instancias")
public class ComentarioConfidencialInstanciaController {

    private final ComentarioConfidencialInstanciaService comentarioService;

    @PostMapping
    @Operation(summary = "Crear comentario confidencial", description = "Crea un comentario confidencial en una instancia. Solo accesible por usuarios con permiso.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comentario creado"),
            @ApiResponse(responseCode = "404", description = "Instancia no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<ComentarioConfidencialInstanciaResponseDTO> crear(
            @PathVariable int codInstancia,
            @RequestBody @Valid ComentarioConfidencialInstanciaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comentarioService.crear(codInstancia, dto));
    }

    @GetMapping
    @Operation(summary = "Listar comentarios confidenciales", description = "Lista todos los comentarios confidenciales activos de una instancia.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista obtenida"),
            @ApiResponse(responseCode = "404", description = "Instancia no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<List<ComentarioConfidencialInstanciaResponseDTO>> listar(
            @PathVariable int codInstancia) {
        return ResponseEntity.ok(comentarioService.listarPorInstancia(codInstancia));
    }

    @PutMapping("/{idComConfidencial}")
    @Operation(summary = "Actualizar comentario confidencial", description = "Edita el contenido de un comentario confidencial activo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comentario actualizado"),
            @ApiResponse(responseCode = "404", description = "Comentario no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<ComentarioConfidencialInstanciaResponseDTO> actualizar(
            @PathVariable int codInstancia,
            @PathVariable int idComConfidencial,
            @RequestBody @Valid ComentarioConfidencialInstanciaRequestDTO dto) {
        return ResponseEntity.ok(comentarioService.actualizar(codInstancia, idComConfidencial, dto));
    }

    @DeleteMapping("/{idComConfidencial}")
    @Operation(summary = "Dar de baja comentario confidencial", description = "Baja lógica de un comentario confidencial.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Comentario dado de baja"),
            @ApiResponse(responseCode = "404", description = "Comentario no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<Void> darDeBaja(
            @PathVariable int codInstancia,
            @PathVariable int idComConfidencial) {
        comentarioService.darDeBaja(codInstancia, idComConfidencial);
        return ResponseEntity.noContent().build();
    }
}