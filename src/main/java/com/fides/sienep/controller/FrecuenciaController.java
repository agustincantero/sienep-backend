package com.fides.sienep.controller;

import com.fides.sienep.dto.FrecuenciaRequestDTO;
import com.fides.sienep.dto.FrecuenciaResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.FrecuenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/frecuencias")
@RequiredArgsConstructor
@Tag(name = "Frecuencias", description = "Gestión de frecuencias de recordatorios")
public class FrecuenciaController {

    private final FrecuenciaService frecuenciaService;

    @Operation(summary = "Crear una frecuencia")
    @ApiResponse(responseCode = "201", description = "Frecuencia creada correctamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @PostMapping
    public ResponseEntity<FrecuenciaResponseDTO> crear(@Valid @RequestBody FrecuenciaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(frecuenciaService.crear(dto));
    }

    @Operation(summary = "Listar frecuencias", description = "Sin el parámetro estado, devuelve solo las ACTIVAS; con estado, filtra por ese valor (ej. ACTIVO, INACTIVO).")
    @ApiResponse(responseCode = "200", description = "Lista de frecuencias")
    @GetMapping
    public ResponseEntity<List<FrecuenciaResponseDTO>> listar(
            @Parameter(description = "Estado opcional para filtrar (ej. ACTIVO, INACTIVO). Por defecto: ACTIVO", required = false)
            @RequestParam(required = false) EstadoLogico estado) {
        return ResponseEntity.ok(frecuenciaService.listar(estado));
    }

    @Operation(summary = "Obtener frecuencia por ID")
    @ApiResponse(responseCode = "200", description = "Frecuencia encontrada")
    @ApiResponse(responseCode = "404", description = "Frecuencia no encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<FrecuenciaResponseDTO> obtenerPorId(@PathVariable int id) {
        return ResponseEntity.ok(frecuenciaService.obtenerPorId(id));
    }

    @Operation(summary = "Editar una frecuencia")
    @ApiResponse(responseCode = "200", description = "Frecuencia editada correctamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Frecuencia no encontrada")
    @PutMapping("/{id}")
    public ResponseEntity<FrecuenciaResponseDTO> editar(@PathVariable int id, @Valid @RequestBody FrecuenciaRequestDTO dto) {
        return ResponseEntity.ok(frecuenciaService.editar(id, dto));
    }

    @Operation(summary = "Desactivar una frecuencia")
    @ApiResponse(responseCode = "204", description = "Frecuencia desactivada correctamente")
    @ApiResponse(responseCode = "404", description = "Frecuencia no encontrada")
    @ApiResponse(responseCode = "409", description = "La frecuencia está en uso por recordatorios activos")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable int id) {
        frecuenciaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivar una frecuencia")
    @ApiResponse(responseCode = "200", description = "Frecuencia reactivada correctamente")
    @ApiResponse(responseCode = "404", description = "Frecuencia no encontrada")
    @ApiResponse(responseCode = "409", description = "La frecuencia ya está activa")
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<FrecuenciaResponseDTO> reactivar(@PathVariable int id) {
        return ResponseEntity.ok(frecuenciaService.reactivar(id));
    }
}