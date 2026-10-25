package com.fides.sienep.controller;

import com.fides.sienep.dto.ComentarioConfidencialEstudianteRequestDTO;
import com.fides.sienep.dto.ComentarioConfidencialEstudianteResponseDTO;
import com.fides.sienep.service.ComentarioConfidencialEstudianteService;
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
@RequestMapping("/comentarios-confidenciales-estudiante")
@RequiredArgsConstructor
@Tag(name = "Comentarios confidenciales de estudiante",
        description = "Gestión de observaciones confidenciales — acceso restringido")
public class ComentarioConfidencialEstudianteController {

    private final ComentarioConfidencialEstudianteService comentarioService;

    @GetMapping
    @Operation(summary = "Listar observaciones confidenciales de un estudiante")
    @ApiResponse(responseCode = "200", description = "Página de observaciones")
    public ResponseEntity<Page<ComentarioConfidencialEstudianteResponseDTO>> listar(
            @RequestParam Integer idEstudiante,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(comentarioService.listarPorEstudiante(idEstudiante, pageable));
    }

    @PostMapping
    @Operation(summary = "Crear observación confidencial sobre un estudiante")
    @ApiResponse(responseCode = "201", description = "Observación creada")
    @ApiResponse(responseCode = "404", description = "Estudiante o funcionario no encontrado")
    public ResponseEntity<ComentarioConfidencialEstudianteResponseDTO> crear(
            @Valid @RequestBody ComentarioConfidencialEstudianteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comentarioService.crear(dto));
    }
}