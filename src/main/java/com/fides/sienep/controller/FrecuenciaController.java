package com.fides.sienep.controller;

import com.fides.sienep.dto.FrecuenciaRequestDTO;
import com.fides.sienep.dto.FrecuenciaResponseDTO;
import com.fides.sienep.service.FrecuenciaService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "Listar frecuencias activas")
    @ApiResponse(responseCode = "200", description = "Lista de frecuencias activas")
    @GetMapping
    public ResponseEntity<List<FrecuenciaResponseDTO>> listar() {
        return ResponseEntity.ok(frecuenciaService.listarActivas());
    }

    @Operation(summary = "Buscar frecuencia por ID")
    @ApiResponse(responseCode = "200", description = "Frecuencia encontrada")
    @ApiResponse(responseCode = "404", description = "Frecuencia no encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<FrecuenciaResponseDTO> buscarPorId(@PathVariable int id) {
        return ResponseEntity.ok(frecuenciaService.buscarPorId(id));
    }

    @Operation(summary = "Actualizar una frecuencia")
    @ApiResponse(responseCode = "200", description = "Frecuencia actualizada correctamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Frecuencia no encontrada")
    @PutMapping("/{id}")
    public ResponseEntity<FrecuenciaResponseDTO> actualizar(@PathVariable int id, @Valid @RequestBody FrecuenciaRequestDTO dto) {
        return ResponseEntity.ok(frecuenciaService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja una frecuencia")
    @ApiResponse(responseCode = "204", description = "Frecuencia dada de baja correctamente")
    @ApiResponse(responseCode = "404", description = "Frecuencia no encontrada")
    @ApiResponse(responseCode = "409", description = "La frecuencia está en uso por recordatorios activos")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable int id) {
        frecuenciaService.darDeBaja(id);
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