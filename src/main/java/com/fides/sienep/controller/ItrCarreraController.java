package com.fides.sienep.controller;

import com.fides.sienep.dto.ItrCarreraRequestDTO;
import com.fides.sienep.dto.ItrCarreraResponseDTO;
import com.fides.sienep.service.ItrCarreraService;
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
@RequestMapping("/api/v1/itr-carrera")
@RequiredArgsConstructor
@Tag(
        name = "ITR-Carrera",
        description = "Gestión de asociaciones entre ITR y Carrera"
)
public class ItrCarreraController {

    private final ItrCarreraService itrCarreraService;


    // CREAR

    @PostMapping
    @Operation(
            summary = "Crear asociación ITR-Carrera",
            description = "Asocia una carrera a un ITR."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Asociación creada correctamente"
    )
    @ApiResponse(
            responseCode = "404",
            description = "ITR o Carrera no encontrados"
    )
    @ApiResponse(
            responseCode = "409",
            description = "La asociación ya existe"
    )
    public ResponseEntity<ItrCarreraResponseDTO> crear(
            @Valid @RequestBody ItrCarreraRequestDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        itrCarreraService.crear(dto)
                );
    }


    // LISTAR

    @GetMapping
    @Operation(
            summary = "Listar asociaciones ITR-Carrera"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado obtenido correctamente"
    )
    public ResponseEntity<List<ItrCarreraResponseDTO>> listar() {

        return ResponseEntity.ok(
                itrCarreraService.listar()
        );
    }


    // OBTENER


    @GetMapping("/{idItr}/{idCarrera}")
    @Operation(
            summary = "Obtener una asociación ITR-Carrera"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Asociación encontrada"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Asociación no encontrada"
    )
    public ResponseEntity<ItrCarreraResponseDTO> obtener(
            @PathVariable Integer idItr,
            @PathVariable Integer idCarrera) {

        return ResponseEntity.ok(
                itrCarreraService.obtener(
                        idItr,
                        idCarrera
                )
        );
    }


    // BAJA LOGICA


    @DeleteMapping("/{idItr}/{idCarrera}")
    @Operation(
            summary = "Dar de baja una asociación ITR-Carrera"
    )
    @ApiResponse(
            responseCode = "204",
            description = "Asociación dada de baja"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Asociación no encontrada"
    )
    public ResponseEntity<Void> eliminar(
            @PathVariable Integer idItr,
            @PathVariable Integer idCarrera) {

        itrCarreraService.eliminar(
                idItr,
                idCarrera
        );

        return ResponseEntity.noContent().build();
    }


    // REACTIVAR


    @PatchMapping("/{idItr}/{idCarrera}/reactivar")
    @Operation(
            summary = "Reactivar una asociación ITR-Carrera"
    )
    @ApiResponse(
            responseCode = "204",
            description = "Asociación reactivada"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Asociación no encontrada"
    )
    public ResponseEntity<Void> reactivar(
            @PathVariable Integer idItr,
            @PathVariable Integer idCarrera) {

        itrCarreraService.reactivar(
                idItr,
                idCarrera
        );

        return ResponseEntity.noContent().build();
    }
}