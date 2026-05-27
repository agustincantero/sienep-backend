package com.fides.sienep.controller;

import com.fides.sienep.dto.InstanciaComunRequestDTO;
import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.service.InstanciaComunService;
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
@RequestMapping("/api/instancias-comunes")
@RequiredArgsConstructor
@Tag(name = "Instancias Comunes", description = "Gestión de categorías asignadas a instancias")
public class InstanciaComunController {

    private final InstanciaComunService instanciaComunService;

    @Operation(summary = "Asignar una categoría a una instancia")
    @ApiResponse(responseCode = "201", description = "Categoría asignada correctamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o categoría ya asignada")
    @ApiResponse(responseCode = "404", description = "Instancia o categoría no encontrada")
    @PostMapping
    public ResponseEntity<InstanciaComunResponseDTO> crear(@Valid @RequestBody InstanciaComunRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(instanciaComunService.crear(dto));
    }

    @Operation(summary = "Listar categorías de una instancia")
    @ApiResponse(responseCode = "200", description = "Lista de categorías de la instancia")
    @GetMapping("/instancia/{codInstancia}")
    public ResponseEntity<List<InstanciaComunResponseDTO>> listarPorInstancia(@PathVariable int codInstancia) {
        return ResponseEntity.ok(instanciaComunService.listarPorInstancia(codInstancia));
    }

    @Operation(summary = "Listar instancias de una categoría")
    @ApiResponse(responseCode = "200", description = "Lista de instancias de la categoría")
    @GetMapping("/categoria/{idCategoria}")
    public ResponseEntity<List<InstanciaComunResponseDTO>> listarPorCategoria(@PathVariable int idCategoria) {
        return ResponseEntity.ok(instanciaComunService.listarPorCategoria(idCategoria));
    }

    @Operation(summary = "Quitar una categoría de una instancia")
    @ApiResponse(responseCode = "204", description = "Categoría quitada correctamente")
    @ApiResponse(responseCode = "404", description = "Relación no encontrada")
    @DeleteMapping("/instancia/{codInstancia}/categoria/{idCategoria}")
    public ResponseEntity<Void> eliminar(@PathVariable int codInstancia, @PathVariable int idCategoria) {
        instanciaComunService.eliminar(codInstancia, idCategoria);
        return ResponseEntity.noContent().build();
    }
}