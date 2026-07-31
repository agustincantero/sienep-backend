package com.fides.sienep.controller;

import com.fides.sienep.dto.IncidenciaRequestDTO;
import com.fides.sienep.dto.IncidenciaResponseDTO;
import com.fides.sienep.service.IncidenciaService;
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

import java.util.List;

@RestController
@RequestMapping("/api/incidencias")
@RequiredArgsConstructor
@Tag(name = "Incidencias", description = "Gestión de incidencias")
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    @Operation(summary = "Crear una incidencia")
    @ApiResponse(responseCode = "201", description = "Incidencia creada correctamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Estudiante o funcionario no encontrado")
    @PostMapping
    public ResponseEntity<IncidenciaResponseDTO> crear(@Valid @RequestBody IncidenciaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidenciaService.crear(dto));
    }

    @Operation(summary = "Buscar incidencia por ID")
    @ApiResponse(responseCode = "200", description = "Incidencia encontrada")
    @ApiResponse(responseCode = "404", description = "Incidencia no encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<IncidenciaResponseDTO> buscarPorId(@PathVariable int id) {
        return ResponseEntity.ok(incidenciaService.buscarPorId(id));
    }

    @Operation(summary = "Listar incidencias, con filtro opcional por estudiante o funcionario")
    @ApiResponse(responseCode = "200", description = "Página de incidencias")
    @ApiResponse(responseCode = "400", description = "Debe indicarse solo un filtro a la vez")
    @GetMapping
    public ResponseEntity<Page<IncidenciaResponseDTO>> listar(
            @RequestParam(required = false) Integer idEstudiante,
            @RequestParam(required = false) Integer idFuncionario,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

        if (idEstudiante != null) {
            return ResponseEntity.ok(incidenciaService.listarPorEstudiante(idEstudiante, pageable));
        }
        if (idFuncionario != null) {
            return ResponseEntity.ok(incidenciaService.listarPorFuncionario(idFuncionario, pageable));
        }
        return ResponseEntity.ok(incidenciaService.listarActivas(pageable));
    }

    @Operation(summary = "Actualizar una incidencia")
    @ApiResponse(responseCode = "200", description = "Incidencia actualizada correctamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Incidencia no encontrada")
    @PutMapping("/{id}")
    public ResponseEntity<IncidenciaResponseDTO> actualizar(@PathVariable int id, @Valid @RequestBody IncidenciaRequestDTO dto) {
        return ResponseEntity.ok(incidenciaService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja una incidencia")
    @ApiResponse(responseCode = "204", description = "Incidencia dada de baja correctamente")
    @ApiResponse(responseCode = "404", description = "Incidencia no encontrada")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable int id) {
        incidenciaService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivar una incidencia")
    @ApiResponse(responseCode = "200", description = "Incidencia reactivada correctamente")
    @ApiResponse(responseCode = "404", description = "Incidencia no encontrada")
    @ApiResponse(responseCode = "409", description = "La incidencia ya está activa")
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<IncidenciaResponseDTO> reactivar(@PathVariable int id) {
        return ResponseEntity.ok(incidenciaService.reactivar(id));
    }

    @Operation(summary = "Ver mis incidencias",
            description = "Permite al estudiante autenticado ver sus propias incidencias activas.")
    @ApiResponse(responseCode = "200", description = "Incidencias obtenidas correctamente")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    @GetMapping("/mis-incidencias")
    public ResponseEntity<List<IncidenciaResponseDTO>> listarMisIncidencias() {
        return ResponseEntity.ok(incidenciaService.listarMisIncidencias());
    }
}