package com.fides.sienep.controller;

import com.fides.sienep.dto.ItrRequestDTO;
import com.fides.sienep.dto.ItrResponseDTO;
import com.fides.sienep.service.ItrService;
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
@RequestMapping("/api/itrs")
@RequiredArgsConstructor
@Tag(name = "ITR", description = "Gestión de ITRs")
public class ItrController {

    private final ItrService itrService;

    @PostMapping
    @Operation(summary = "Crear ITR", description = "Registra un nuevo ITR y lo asocia opcionalmente a carreras.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "ITR creado con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "El nombre del ITR ya está en uso")
    })
    public ResponseEntity<ItrResponseDTO> crear(@Valid @RequestBody ItrRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itrService.crear(dto));
    }

    @GetMapping
    @Operation(summary = "Listar ITRs", description = "Retorna una lista de todos los ITRs junto con sus carreras activas (optimizado con caché).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    })
    public ResponseEntity<List<ItrResponseDTO>> listar() {
        return ResponseEntity.ok(itrService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener ITR por ID", description = "Busca y retorna los datos de un ITR específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ITR encontrado y retornado"),
            @ApiResponse(responseCode = "404", description = "ITR no encontrado")
    })
    public ResponseEntity<ItrResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(itrService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar ITR", description = "Modifica los datos de un ITR existente y actualiza sus asociaciones de carreras.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ITR actualizado con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "ITR o Carrera no encontrada"),
            @ApiResponse(responseCode = "409", description = "ITR inactivo o nombre en uso")
    })
    public ResponseEntity<ItrResponseDTO> actualizar(@PathVariable Integer id, @Valid @RequestBody ItrRequestDTO dto) {
        return ResponseEntity.ok(itrService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja ITR", description = "Aplica una baja lógica (estado INACTIVO) a un ITR.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "ITR dado de baja exitosamente"),
            @ApiResponse(responseCode = "404", description = "ITR no encontrado")
    })
    public ResponseEntity<Void> darDeBaja(@PathVariable Integer id) {
        itrService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivar")
    @Operation(summary = "Reactivar ITR", description = "Restaura el estado de un ITR inactivo a ACTIVO.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "ITR reactivado exitosamente"),
            @ApiResponse(responseCode = "404", description = "ITR no encontrado")
    })
    public ResponseEntity<Void> reactivar(@PathVariable Integer id) {
        itrService.reactivar(id);
        return ResponseEntity.noContent().build();
    }
}