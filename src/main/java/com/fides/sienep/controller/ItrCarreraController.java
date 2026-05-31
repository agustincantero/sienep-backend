package com.fides.sienep.controller;

import com.fides.sienep.dto.CambiarEstadoDTO;
import com.fides.sienep.dto.ItrCarreraRequestDTO;
import com.fides.sienep.dto.ItrCarreraResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.ItrCarreraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/itr-carreras")
@RequiredArgsConstructor
@Tag(
        name = "ITR-Carrera",
        description = "Gestión de asociaciones entre ITR y Carreras"
)
public class ItrCarreraController {

    private final ItrCarreraService itrCarreraService;

    /* ******************************
             CREAR
    * ******************************/

    @PostMapping
    @Operation(
            summary = "Asociar una carrera a un ITR",
            description = "Crea una nueva relación ITR-Carrera."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Relación creada correctamente"
    )
    @ApiResponse(
            responseCode = "409",
            description = "La relación ya existe"
    )
    @ApiResponse(
            responseCode = "404",
            description = "ITR o Carrera no encontrados"
    )
    public ResponseEntity<ItrCarreraResponseDTO> crear(
            @Valid @RequestBody ItrCarreraRequestDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(itrCarreraService.crear(dto));
    }

    /* ******************************
                 LISTAR
   * ******************************/

    @GetMapping
    @Operation(
            summary = "Listar asociaciones ITR-Carrera",
            description = "Devuelve todas las asociaciones registradas."
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

    /* ******************************
             OBTENER
    * ******************************/

    @GetMapping("/{idItr}/{idCarrera}")
    @Operation(
            summary = "Obtener una asociación ITR-Carrera",
            description = "Busca una relación específica mediante sus IDs."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Relación encontrada"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Relación no encontrada"
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

    /* ******************************
             BAJA LÓGICA
    * ******************************/

    @DeleteMapping("/{idItr}/{idCarrera}")
    @Operation(
            summary = "Dar de baja una asociación ITR-Carrera",
            description = "Realiza baja lógica cambiando el estado a INACTIVO."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Relación dada de baja"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Relación no encontrada"
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

    /* ******************************
                 REACTIVAR
    * ******************************/

    @PatchMapping("/{idItr}/{idCarrera}/estado")
    @Operation(
            summary = "Reactivar asociación ITR-Carrera",
            description = "Solo acepta estado = ACTIVO."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Relación reactivada"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Estado inválido"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Relación no encontrada"
    )
    public ResponseEntity<Void> reactivar(
            @PathVariable Integer idItr,
            @PathVariable Integer idCarrera,
            @Valid @RequestBody CambiarEstadoDTO dto) {

        if (dto.getEstado() != EstadoLogico.ACTIVO) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Este endpoint solo acepta estado = ACTIVO."
            );
        }

        itrCarreraService.reactivar(
                idItr,
                idCarrera
        );

        return ResponseEntity.noContent().build();
    }
}