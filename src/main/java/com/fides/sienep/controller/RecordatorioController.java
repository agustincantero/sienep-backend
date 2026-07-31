package com.fides.sienep.controller;

import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.dto.InstanciaDesdeRecordatorioDTO;
import com.fides.sienep.dto.RecordatorioRequestDTO;
import com.fides.sienep.dto.RecordatorioResponseDTO;
import com.fides.sienep.service.InstanciaComunService;
import com.fides.sienep.service.RecordatorioService;
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
@RequestMapping("/api/recordatorios")
@RequiredArgsConstructor
@Tag(name = "Recordatorios", description = "Gestión de recordatorios")
public class RecordatorioController {

    private final RecordatorioService recordatorioService;
    private final InstanciaComunService instanciaComunService;

    @Operation(summary = "Crear un recordatorio")
    @ApiResponse(responseCode = "201", description = "Recordatorio creado correctamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Instancia, categoría, responsable o frecuencia no encontrada")
    @PostMapping
    public ResponseEntity<RecordatorioResponseDTO> crear(@Valid @RequestBody RecordatorioRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recordatorioService.crear(dto));
    }

    @Operation(summary = "Listar recordatorios activos")
    @ApiResponse(responseCode = "200", description = "Página de recordatorios activos")
    @GetMapping
    public ResponseEntity<Page<RecordatorioResponseDTO>> listar(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(recordatorioService.listarActivos(pageable));
    }

    @Operation(summary = "Buscar recordatorio por ID")
    @ApiResponse(responseCode = "200", description = "Recordatorio encontrado")
    @ApiResponse(responseCode = "404", description = "Recordatorio no encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<RecordatorioResponseDTO> buscarPorId(@PathVariable int id) {
        return ResponseEntity.ok(recordatorioService.buscarPorId(id));
    }

    @Operation(summary = "Listar recordatorios por instancia")
    @ApiResponse(responseCode = "200", description = "Página de recordatorios de la instancia")
    @GetMapping("/instancia/{codInstancia}")
    public ResponseEntity<Page<RecordatorioResponseDTO>> listarPorInstancia(
            @PathVariable int codInstancia,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(recordatorioService.listarPorInstancia(codInstancia, pageable));
    }

    @Operation(summary = "Listar recordatorios por responsable")
    @ApiResponse(responseCode = "200", description = "Página de recordatorios del responsable")
    @GetMapping("/responsable/{idResponsable}")
    public ResponseEntity<Page<RecordatorioResponseDTO>> listarPorResponsable(
            @PathVariable int idResponsable,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(recordatorioService.listarPorResponsable(idResponsable, pageable));
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

    @Operation(summary = "Reactivar un recordatorio")
    @ApiResponse(responseCode = "200", description = "Recordatorio reactivado correctamente")
    @ApiResponse(responseCode = "404", description = "Recordatorio no encontrado")
    @ApiResponse(responseCode = "409", description = "El recordatorio ya está activo")
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<RecordatorioResponseDTO> reactivar(@PathVariable int id) {
        return ResponseEntity.ok(recordatorioService.reactivar(id));
    }

    @Operation(summary = "Crear instancia desde un recordatorio cumplido",
            description = "Genera una instancia de seguimiento a partir de un recordatorio activo, " +
                    "usando el mismo estudiante que la instancia de origen del recordatorio. " +
                    "Marca el recordatorio como cumplido (RF27). No se puede volver a usar el " +
                    "mismo recordatorio una vez que ya generó una instancia. No es un recurso " +
                    "anidado puro (no existe un GET equivalente): es una acción de negocio con " +
                    "un efecto secundario, nombrada como sub-recurso plural por legibilidad.")
    @ApiResponse(responseCode = "201", description = "Instancia creada correctamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Recordatorio o categoría no encontrada")
    @ApiResponse(responseCode = "409", description = "El recordatorio ya generó una instancia")
    @PostMapping("/{id}/instancias")
    public ResponseEntity<InstanciaComunResponseDTO> crearInstanciaDesdeRecordatorio(
            @PathVariable int id,
            @Valid @RequestBody InstanciaDesdeRecordatorioDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(instanciaComunService.crearDesdeRecordatorio(id, dto));
    }
}
