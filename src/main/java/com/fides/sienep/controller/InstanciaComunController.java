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

    @Operation(summary = "Listar instancias comunes")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @ApiResponse(responseCode = "400", description = "Debe indicarse al menos un parámetro de búsqueda")
    @GetMapping
    public ResponseEntity<List<InstanciaComunResponseDTO>> listar(
            @RequestParam(required = false) Integer codInstancia,
            @RequestParam(required = false) Integer idCategoria) {

        if (codInstancia != null) {
            return ResponseEntity.ok(instanciaComunService.listarPorInstancia(codInstancia));
        }
        if (idCategoria != null) {
            return ResponseEntity.ok(instanciaComunService.listarPorCategoria(idCategoria));
        }
        return ResponseEntity.badRequest().build();
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