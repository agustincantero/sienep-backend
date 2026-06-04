package com.fides.sienep.controller;

import com.fides.sienep.dto.CarreraRequestDTO;
import com.fides.sienep.dto.CarreraResponseDTO;
import com.fides.sienep.service.CarreraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carreras")
@RequiredArgsConstructor
@Tag(name = "Carreras", description = "Gestión de carreras")
public class CarreraController {

    private final CarreraService carreraService;


    // CREAR

    @PostMapping
    @Operation(
            summary = "Crear carrera",
            description = "Registra una nueva carrera."
    )
    @ApiResponse(responseCode = "201", description = "Carrera creada")
    @ApiResponse(responseCode = "409", description = "Ya existe una carrera con ese nombre")
    public ResponseEntity<CarreraResponseDTO> crear(
            @Valid @RequestBody CarreraRequestDTO dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carreraService.crear(dto));
    }


    /* LISTAR
    // =========================================================

    @GetMapping
    @Operation(
            summary = "Listar carreras",
            description = "Devuelve todas las carreras."
    )
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    public ResponseEntity<List<CarreraResponseDTO>> listar() {
        return ResponseEntity.ok(carreraService.listar());
    }*/


    // OBTENER POR ID


    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener carrera por ID",
            description = "Devuelve una carrera específica."
    )
    @ApiResponse(responseCode = "200", description = "Carrera encontrada")
    @ApiResponse(responseCode = "404", description = "Carrera no encontrada")
    public ResponseEntity<CarreraResponseDTO> obtenerPorId(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                carreraService.obtenerPorId(id)
        );
    }


    // ACTUALIZAR

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar carrera",
            description = "Actualiza el nombre de una carrera."
    )
    @ApiResponse(responseCode = "200", description = "Carrera actualizada")
    @ApiResponse(responseCode = "404", description = "Carrera no encontrada")
    public ResponseEntity<CarreraResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody CarreraRequestDTO dto) {

        return ResponseEntity.ok(
                carreraService.actualizar(id, dto)
        );
    }


    // BAJA LÓGICA


    @DeleteMapping("/{id}")
    @Operation(
            summary = "Dar de baja una carrera",
            description = "Cambia el estado de la carrera a INACTIVO."
    )
    @ApiResponse(responseCode = "204", description = "Carrera dada de baja")
    @ApiResponse(responseCode = "404", description = "Carrera no encontrada")
    public ResponseEntity<Void> darDeBaja(
            @PathVariable Integer id) {

        carreraService.eliminar(id);

        return ResponseEntity.noContent().build();
    }


    /* REACTIVAR


    @PatchMapping("/{id}/estado")
    @Operation(
            summary = "Reactivar carrera",
            description = "Permite reactivar una carrera inactiva."
    )
    @ApiResponse(responseCode = "204", description = "Carrera reactivada")
    @ApiResponse(responseCode = "400", description = "Estado inválido")
    @ApiResponse(responseCode = "404", description = "Carrera no encontrada")
    public ResponseEntity<Void> reactivar(
            @PathVariable Integer id,
            @Valid @RequestBody CambiarEstadoDTO dto) {

        if (dto.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se admite estado ACTIVO para este endpoint."
            );
        }

        carreraService.reactivar(id);

        return ResponseEntity.noContent().build();
    }*/
}