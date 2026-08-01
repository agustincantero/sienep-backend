package com.fides.sienep.controller;

import com.fides.sienep.dto.CarreraRequestDTO;
import com.fides.sienep.dto.CarreraResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.CarreraService;
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
@RequestMapping("/api/carreras")
@RequiredArgsConstructor
@Tag(name = "Carreras", description = "Gestión de carreras")
public class CarreraController {

    private final CarreraService carreraService;

    @PostMapping
    @Operation(summary = "Crear carrera", description = "Registra una nueva carrera.")
    @ApiResponse(responseCode = "201", description = "Carrera creada")
    @ApiResponse(responseCode = "409", description = "Ya existe una carrera con ese nombre")
    public ResponseEntity<CarreraResponseDTO> crear(@Valid @RequestBody CarreraRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carreraService.crear(dto));
    }

    @GetMapping
    @Operation(summary = "Listar carreras", description = "Devuelve las carreras. Sin el parámetro estado, devuelve solo las ACTIVAS.")
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    public ResponseEntity<List<CarreraResponseDTO>> listar(
            @Parameter(description = "Estado opcional para filtrar (ej. ACTIVO, INACTIVO). Por defecto: ACTIVO", required = false)
            @RequestParam(required = false) EstadoLogico estado) {
        return ResponseEntity.ok(carreraService.listar(estado));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener carrera por ID", description = "Devuelve una carrera específica.")
    @ApiResponse(responseCode = "200", description = "Carrera encontrada")
    @ApiResponse(responseCode = "404", description = "Carrera no encontrada")
    public ResponseEntity<CarreraResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(carreraService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar carrera", description = "Edita el nombre de una carrera.")
    @ApiResponse(responseCode = "200", description = "Carrera editada")
    @ApiResponse(responseCode = "404", description = "Carrera no encontrada")
    public ResponseEntity<CarreraResponseDTO> editar(@PathVariable Integer id, @Valid @RequestBody CarreraRequestDTO dto) {
        return ResponseEntity.ok(carreraService.editar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar una carrera", description = "Cambia el estado de la carrera a INACTIVO.")
    @ApiResponse(responseCode = "204", description = "Carrera desactivada")
    @ApiResponse(responseCode = "404", description = "Carrera no encontrada")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        carreraService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivar")
    @Operation(summary = "Reactivar carrera", description = "Permite reactivar una carrera inactiva.")
    @ApiResponse(responseCode = "204", description = "Carrera reactivada")
    @ApiResponse(responseCode = "404", description = "Carrera no encontrada")
    public ResponseEntity<Void> reactivar(@PathVariable Integer id) {
        carreraService.reactivar(id);
        return ResponseEntity.noContent().build();
    }
}