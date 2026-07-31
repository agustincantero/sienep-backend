package com.fides.sienep.controller;

import com.fides.sienep.dto.ComentarioNormalInstanciaRequestDTO;
import com.fides.sienep.dto.ComentarioNormalInstanciaResponseDTO;
import com.fides.sienep.service.ComentarioNormalInstanciaService;
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
@RequestMapping("/api/instancias/{codInstancia}/comentarios-normales")
@RequiredArgsConstructor
@Tag(name = "Comentarios Normales de Instancia", description = "Gestión de comentarios normales de instancias")
public class ComentarioNormalInstanciaController {

    private final ComentarioNormalInstanciaService comentarioService;

    @PostMapping
    @Operation(summary = "Crear comentario normal", description = "Crea un comentario normal en una instancia. El autor es el usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comentario creado"),
            @ApiResponse(responseCode = "404", description = "Instancia no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<ComentarioNormalInstanciaResponseDTO> crear(
            @PathVariable int codInstancia,
            @RequestBody @Valid ComentarioNormalInstanciaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comentarioService.crear(codInstancia, dto));
    }

    @GetMapping
    @Operation(summary = "Listar comentarios normales", description = "Lista todos los comentarios normales activos de una instancia.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista obtenida"),
            @ApiResponse(responseCode = "404", description = "Instancia no encontrada")
    })
    public ResponseEntity<List<ComentarioNormalInstanciaResponseDTO>> listar(
            @PathVariable int codInstancia) {
        return ResponseEntity.ok(comentarioService.listarPorInstancia(codInstancia));
    }

    @PutMapping("/{idComentario}")
    @Operation(summary = "Actualizar comentario normal", description = "Edita el contenido de un comentario normal activo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comentario actualizado"),
            @ApiResponse(responseCode = "404", description = "Comentario no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<ComentarioNormalInstanciaResponseDTO> actualizar(
            @PathVariable int codInstancia,
            @PathVariable int idComentario,
            @RequestBody @Valid ComentarioNormalInstanciaRequestDTO dto) {
        return ResponseEntity.ok(comentarioService.actualizar(codInstancia, idComentario, dto));
    }

    @DeleteMapping("/{idComentario}")
    @Operation(summary = "Dar de baja comentario normal", description = "Baja lógica de un comentario normal.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Comentario dado de baja"),
            @ApiResponse(responseCode = "404", description = "Comentario no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<Void> darDeBaja(
            @PathVariable int codInstancia,
            @PathVariable int idComentario) {
        comentarioService.darDeBaja(codInstancia, idComentario);
        return ResponseEntity.noContent().build();
    }
}