package com.fides.sienep.controller;

import com.fides.sienep.dto.FuncionarioRequestDTO;
import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.service.FuncionarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

// ─────────────────────────────────────────────────────────────
// CAMBIO 1 — eliminamos @Autowired, agregamos @RequiredArgsConstructor
//
// Mismo razonamiento que en el Service. El Controller tampoco
// necesita @Autowired cuando usa constructor injection vía Lombok.
// ─────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────
    // CAMBIO 2 — ResponseEntity en todos los métodos
    //
    // Antes: los métodos devolvían el DTO directamente. Spring
    // siempre respondía HTTP 200, incluso en un POST donde el
    // correcto es 201 Created.
    //
    // ResponseEntity<T> nos da control explícito sobre:
    //   - El código de estado HTTP (200, 201, 204, 404...).
    //   - Los headers de respuesta si hiciera falta.
    //   - El cuerpo (body) de la respuesta.
    //
    // Patrones usados:
    //   ResponseEntity.ok(body)          → 200 con body
    //   ResponseEntity.status(201).body  → 201 con body
    //   ResponseEntity.noContent().build → 204 sin body
    // ─────────────────────────────────────────────────────────

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
    public ResponseEntity<FuncionarioResponseDTO> crear(@RequestBody FuncionarioRequestDTO dto) {

        FuncionarioResponseDTO creado = funcionarioService.crear(dto);

        // 201 Created: la convención HTTP dice que crear un recurso
        // nuevo debe responder 201, no 200.
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(summary = "Actualizar funcionario")
    @ApiResponse(responseCode = "200", description = "Funcionario actualizado correctamente")
    @ApiResponse(responseCode = "404", description = "Funcionario no encontrado")
    @PutMapping("/{id}")
    public ResponseEntity<FuncionarioResponseDTO> actualizar(
            @PathVariable Integer id,
            @RequestBody FuncionarioRequestDTO dto) {

        return ResponseEntity.ok(funcionarioService.actualizar(id, dto));
    }

    @Operation(summary = "Eliminar funcionario")
    @ApiResponse(responseCode = "204", description = "Funcionario eliminado correctamente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {

        funcionarioService.eliminar(id);

        // 204 No Content: la operación fue exitosa pero no hay
        // nada que devolver en el body (ya no existe el recurso).
        return ResponseEntity.noContent().build();
    }
}