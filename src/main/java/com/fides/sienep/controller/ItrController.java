package com.fides.sienep.controller;

import com.fides.sienep.dto.CambiarEstadoDTO;
import com.fides.sienep.dto.ItrRequestDTO;
import com.fides.sienep.dto.ItrResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.ItrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/itrs")
@RequiredArgsConstructor
@Tag(name = "ITR", description = "Gestión de ITRs")
public class ItrController {

    private final ItrService itrService;

    @PostMapping
    @Operation(summary = "Crear ITR")
    public ResponseEntity<ItrResponseDTO> crear(
            @Valid @RequestBody ItrRequestDTO dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itrService.crear(dto));
    }

    @GetMapping
    @Operation(summary = "Listar ITRs")
    public ResponseEntity<List<ItrResponseDTO>> listar() {

        return ResponseEntity.ok(
                itrService.listar()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener ITR por ID")
    public ResponseEntity<ItrResponseDTO> obtenerPorId(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                itrService.obtenerPorId(id)
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar ITR")
    public ResponseEntity<ItrResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ItrRequestDTO dto) {

        return ResponseEntity.ok(
                itrService.actualizar(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja ITR")
    public ResponseEntity<Void> darDeBaja(
            @PathVariable Integer id) {

        itrService.darDeBaja(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Reactivar ITR")
    public ResponseEntity<Void> reactivar(
            @PathVariable Integer id,
            @Valid @RequestBody CambiarEstadoDTO dto) {

        if (dto.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se admite ACTIVO."
            );
        }

        itrService.reactivar(id);

        return ResponseEntity.noContent().build();
    }
}