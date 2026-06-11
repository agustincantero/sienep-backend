package com.fides.sienep.controller;

import com.fides.sienep.dto.InstanciaComunRequestDTO;
import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.dto.InstanciaComunUpdateDTO;
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
@RequestMapping("/api/instancias")
@RequiredArgsConstructor
@Tag(name = "Instancias", description = "Gestión de instancias formales (reuniones, llamadas, seguimientos)")
public class InstanciaComunController {

    private final InstanciaComunService instanciaComunService;

    @Operation(summary = "Crear una instancia")
    @ApiResponse(responseCode = "201", description = "Instancia creada correctamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Estudiante, funcionario o categoría no encontrada")
    @PostMapping
    public ResponseEntity<InstanciaComunResponseDTO> crear(
            @Valid @RequestBody InstanciaComunRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(instanciaComunService.crear(dto));
    }

    @Operation(summary = "Buscar instancia por ID")
    @ApiResponse(responseCode = "200", description = "Instancia encontrada")
    @ApiResponse(responseCode = "404", description = "Instancia no encontrada")
    @GetMapping("/{codInstancia}")
    public ResponseEntity<InstanciaComunResponseDTO> buscarPorId(@PathVariable int codInstancia) {
        return ResponseEntity.ok(instanciaComunService.buscarPorId(codInstancia));
    }

    @Operation(summary = "Listar instancias con filtros opcionales")
    @ApiResponse(responseCode = "200", description = "Lista de instancias")
    @GetMapping
    public ResponseEntity<List<InstanciaComunResponseDTO>> listar(
            @RequestParam(required = false) Integer idEstudiante,
            @RequestParam(required = false) Integer idFuncionario,
            @RequestParam(required = false) Integer idCategoria) {

        if (idEstudiante != null) {
            return ResponseEntity.ok(instanciaComunService.listarPorEstudiante(idEstudiante));
        }
        if (idFuncionario != null) {
            return ResponseEntity.ok(instanciaComunService.listarPorFuncionario(idFuncionario));
        }
        if (idCategoria != null) {
            return ResponseEntity.ok(instanciaComunService.listarPorCategoria(idCategoria));
        }
        return ResponseEntity.ok(instanciaComunService.listarActivas());
    }

    @Operation(summary = "Actualizar una instancia")
    @ApiResponse(responseCode = "200", description = "Instancia actualizada correctamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Instancia o categoría no encontrada")
    @PutMapping("/{codInstancia}")
    public ResponseEntity<InstanciaComunResponseDTO> actualizar(
            @PathVariable int codInstancia,
            @Valid @RequestBody InstanciaComunUpdateDTO dto) {
        return ResponseEntity.ok(instanciaComunService.actualizar(codInstancia, dto));
    }

    @Operation(summary = "Dar de baja una instancia")
    @ApiResponse(responseCode = "204", description = "Instancia dada de baja correctamente")
    @ApiResponse(responseCode = "404", description = "Instancia no encontrada")
    @DeleteMapping("/{codInstancia}")
    public ResponseEntity<Void> darDeBaja(@PathVariable int codInstancia) {
        instanciaComunService.darDeBaja(codInstancia);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ver mis instancias",
            description = "Permite al estudiante autenticado ver sus propias instancias activas.")
    @ApiResponse(responseCode = "200", description = "Instancias obtenidas correctamente")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    @GetMapping("/mis-instancias")
    public ResponseEntity<List<InstanciaComunResponseDTO>> listarMisInstancias() {
        return ResponseEntity.ok(instanciaComunService.listarMisInstancias());
    }
}