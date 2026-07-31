package com.fides.sienep.controller;

import com.fides.sienep.dto.InvolucradoRequestDTO;
import com.fides.sienep.dto.InvolucradoResponseDTO;
import com.fides.sienep.service.InvolucradoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidencias/{codInstancia}/involucrados")
@RequiredArgsConstructor
@Tag(name = "Involucrados",
        description = "Gestión de involucrados externos en instancias")
public class InvolucradoController {

    private final InvolucradoService involucradoService;

    @PostMapping
    @Operation(summary = "Agregar involucrado",
            description = "Agrega un involucrado externo a una instancia.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Involucrado agregado"),
            @ApiResponse(responseCode = "404", description = "Instancia no encontrada"),
            @ApiResponse(responseCode = "409", description = "Involucrado ya existe en esta instancia")
    })
    public ResponseEntity<InvolucradoResponseDTO> agregar(
            @PathVariable int codInstancia,
            @Valid @RequestBody InvolucradoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(involucradoService.agregar(codInstancia, dto));
    }

    @GetMapping
    @Operation(summary = "Listar involucrados",
            description = "Lista los involucrados activos de una instancia.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido"),
            @ApiResponse(responseCode = "404", description = "Instancia no encontrada")
    })
    public ResponseEntity<List<InvolucradoResponseDTO>> listar(
            @PathVariable int codInstancia) {
        return ResponseEntity.ok(involucradoService.listar(codInstancia));
    }

    @DeleteMapping("/{nomInvolucrado}")
    @Operation(summary = "Desactivar involucrado",
            description = "Baja lógica del involucrado (estado INACTIVO).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Baja realizada"),
            @ApiResponse(responseCode = "404", description = "Involucrado no encontrado")
    })
    public ResponseEntity<Void> desactivar(
            @PathVariable int codInstancia,
            @PathVariable String nomInvolucrado) {
        involucradoService.desactivar(codInstancia, nomInvolucrado);
        return ResponseEntity.noContent().build();
    }
}