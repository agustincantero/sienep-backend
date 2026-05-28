package com.fides.sienep.controller;

import com.fides.sienep.dto.RecordatorioRequestDTO;
import com.fides.sienep.dto.RecordatorioResponseDTO;
import com.fides.sienep.service.RecordatorioService;
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
@RequestMapping("/api/recordatorios")
@RequiredArgsConstructor
@Tag(name = "Recordatorios", description = "Gestión de recordatorios")
public class RecordatorioController {

    private final RecordatorioService recordatorioService;

    @Operation(summary = "Crear un recordatorio")
    @ApiResponse(responseCode = "201", description = "Recordatorio creado correctamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Instancia, categoría, responsable o frecuencia no encontrada")
    @PostMapping
    public ResponseEntity<RecordatorioResponseDTO> crear(@Valid @RequestBody RecordatorioRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recordatorioService.crear(dto));
    }

    @Operation(summary = "Listar recordatorios activos")
    @ApiResponse(responseCode = "200", description = "Lista de recordatorios activos")
    @GetMapping
    public ResponseEntity<List<RecordatorioResponseDTO>> listar() {
        return ResponseEntity.ok(recordatorioService.listarActivos());
    }

    @Operation(summary = "Buscar recordatorio por ID")
    @ApiResponse(responseCode = "200", description = "Recordatorio encontrado")
    @ApiResponse(responseCode = "404", description = "Recordatorio no encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<RecordatorioResponseDTO> buscarPorId(@PathVariable int id) {
        return ResponseEntity.ok(recordatorioService.buscarPorId(id));
    }

    @Operation(summary = "Listar recordatorios por instancia")
    @ApiResponse(responseCode = "200", description = "Lista de recordatorios de la instancia")
    @GetMapping("/instancia/{codInstancia}")
    public ResponseEntity<List<RecordatorioResponseDTO>> listarPorInstancia(@PathVariable int codInstancia) {
        return ResponseEntity.ok(recordatorioService.listarPorInstancia(codInstancia));
    }

    @Operation(summary = "Listar recordatorios por responsable")
    @ApiResponse(responseCode = "200", description = "Lista de recordatorios del responsable")
    @GetMapping("/responsable/{idResponsable}")
    public ResponseEntity<List<RecordatorioResponseDTO>> listarPorResponsable(@PathVariable int idResponsable) {
        return ResponseEntity.ok(recordatorioService.listarPorResponsable(idResponsable));
    }

    @Operation(summary = "Actualizar un recordatorio")
    @ApiResponse(responseCode = "200", description = "Recordatorio actualizado correctamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Recordatorio no encontrado")
    @PutMapping("/{id}")
    public ResponseEntity<RecordatorioResponseDTO> actualizar(@PathVariable int id, @Valid @RequestBody RecordatorioRequestDTO dto) {
        return ResponseEntity.ok(recordatorioService.actualizar(id, dto));
    }

    @Operation(summary = "Dar de baja un recordatorio")
    @ApiResponse(responseCode = "204", description = "Recordatorio dado de baja correctamente")
    @ApiResponse(responseCode = "404", description = "Recordatorio no encontrado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable int id) {
        recordatorioService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }
}