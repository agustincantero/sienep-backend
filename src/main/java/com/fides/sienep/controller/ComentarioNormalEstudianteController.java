package com.fides.sienep.controller;

import com.fides.sienep.dto.ComentarioNormalEstudianteRequestDTO;
import com.fides.sienep.dto.ComentarioNormalEstudianteResponseDTO;
import com.fides.sienep.service.ComentarioNormalEstudianteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comentarios-normales-estudiante")
@RequiredArgsConstructor
@Tag(name = "Comentarios normales de estudiante",
        description = "Gestión de comentarios observables sobre estudiantes")
public class ComentarioNormalEstudianteController {

    private final ComentarioNormalEstudianteService comentarioService;

    @GetMapping
    @Operation(summary = "Listar comentarios de un estudiante",
            description = "Funcionarios indican idEstudiante como parámetro. " +
                    "Estudiantes autenticados ven automáticamente sus propios comentarios.")
    @ApiResponse(responseCode = "200", description = "Página de comentarios")
    @ApiResponse(responseCode = "400", description = "Falta el parámetro idEstudiante (solo para funcionarios)")
    public ResponseEntity<Page<ComentarioNormalEstudianteResponseDTO>> listar(
            @RequestParam(required = false) Integer idEstudiante,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(comentarioService.listarPorEstudiante(idEstudiante, pageable));
    }

    @PostMapping
    @Operation(summary = "Crear comentario normal sobre un estudiante")
    @ApiResponse(responseCode = "201", description = "Comentario creado")
    @ApiResponse(responseCode = "404", description = "Estudiante o funcionario no encontrado")
    public ResponseEntity<ComentarioNormalEstudianteResponseDTO> crear(
            @Valid @RequestBody ComentarioNormalEstudianteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comentarioService.crear(dto));
    }
}