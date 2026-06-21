package com.fides.sienep.controller;

import com.fides.sienep.dto.FuncionarioRequestDTO;
import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.dto.FuncionarioUpdateDTO;
import com.fides.sienep.service.FuncionarioService;
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
@RequestMapping("/api/funcionarios")
@RequiredArgsConstructor
@Tag(name = "Funcionarios", description = "Gestión de funcionarios")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    @Operation(summary = "Obtener todos los funcionarios")
    @ApiResponse(responseCode = "200", description = "Funcionarios obtenidos correctamente")
    @GetMapping
    public ResponseEntity<List<FuncionarioResponseDTO>> obtenerTodos() {

        return ResponseEntity.ok(funcionarioService.obtenerTodos());
    }

    @Operation(summary = "Obtener funcionario por ID")
    @ApiResponse(responseCode = "200", description = "Funcionario encontrado")
    @ApiResponse(responseCode = "404", description = "Funcionario no encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<FuncionarioResponseDTO> obtenerPorId(@PathVariable Integer id) {

        // El servicio ya lanza 404 si no existe.
        // El controller solo envuelve el resultado exitoso en 200.
        return ResponseEntity.ok(funcionarioService.obtenerPorId(id));
    }

    @Operation(summary = "Crear funcionario")
    @ApiResponse(responseCode = "201", description = "Funcionario creado correctamente")
    @PostMapping
    public ResponseEntity<FuncionarioResponseDTO> crear(@Valid @RequestBody FuncionarioRequestDTO dto) {

        FuncionarioResponseDTO creado = funcionarioService.crear(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(summary = "Actualizar funcionario")
    @ApiResponse(responseCode = "200", description = "Funcionario actualizado correctamente")
    @ApiResponse(responseCode = "404", description = "Funcionario no encontrado")
    @PutMapping("/{id}")
    public ResponseEntity<FuncionarioResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody FuncionarioUpdateDTO dto) {

        return ResponseEntity.ok(funcionarioService.actualizar(id, dto));
    }

    @Operation(summary = "Eliminar funcionario")
    @ApiResponse(responseCode = "204", description = "Funcionario eliminado correctamente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        funcionarioService.darDeBaja(id);

        // 204 No Content: la operación fue exitosa pero no hay nada que devolver en el body (ya no existe el recurso).
        return ResponseEntity.noContent().build();
    }

    // ========================================================
    //SOLE
    @Operation(summary = "Reactivar funcionario")
    @ApiResponse(
            responseCode = "200",
            description = "Funcionario reactivado correctamente"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Funcionario no encontrado"
    )
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<FuncionarioResponseDTO> reactivar(@PathVariable Integer id) {
        return ResponseEntity.ok(funcionarioService.reactivar(id));
    }

    // ========================================================


    @Operation(summary = "Obtener funcionario por documento")
    @ApiResponse(responseCode = "200", description = "Funcionario encontrado")
    @ApiResponse(responseCode = "404", description = "Funcionario no encontrado")
    @GetMapping("/documento/{documento}")
    public ResponseEntity<FuncionarioResponseDTO> obtenerPorDocumento(@PathVariable String documento) {
        return ResponseEntity.ok(funcionarioService.obtenerPorDocumento(documento));
    }
}