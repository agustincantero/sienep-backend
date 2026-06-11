package com.fides.sienep.controller;

import com.fides.sienep.dto.FuncionarioRequestDTO;
import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.service.FuncionarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PatchMapping;

import java.util.List;

@RestController
@RequestMapping("/api/funcionarios")
public class FuncionarioController {

    @Autowired
    private FuncionarioService funcionarioService;

    @Operation(summary = "Obtener todos los funcionarios")
    @ApiResponse(
            responseCode = "200",
            description = "Funcionarios obtenidos correctamente"
    )
    @GetMapping
    public List<FuncionarioResponseDTO> obtenerTodos() {

        return funcionarioService.obtenerTodos();
    }

    @Operation(summary = "Obtener funcionario por ID")
    @ApiResponse(
            responseCode = "200",
            description = "Funcionario encontrado"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Funcionario no encontrado"
    )
    @GetMapping("/{id}")
    public FuncionarioResponseDTO obtenerPorId(
            @PathVariable Integer id
    ) {

        return funcionarioService.obtenerPorId(id);
    }

    @Operation(summary = "Crear funcionario")
    @ApiResponse(
            responseCode = "201",
            description = "Funcionario creado correctamente"
    )
    @PostMapping
    public FuncionarioResponseDTO crear(
            @Valid @RequestBody FuncionarioRequestDTO dto
    ) {

        return funcionarioService.crear(dto);
    }

    @Operation(summary = "Actualizar funcionario")
    @ApiResponse(
            responseCode = "200",
            description = "Funcionario actualizado correctamente"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Funcionario no encontrado"
    )
    @PutMapping("/{id}")
    public FuncionarioResponseDTO actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody FuncionarioRequestDTO dto
    ) {

        return funcionarioService.actualizar(id, dto);
    }

    @Operation(summary = "Eliminar funcionario")
    @ApiResponse(
            responseCode = "200",
            description = "Funcionario dado de baja correctamente"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Funcionario no encontrado"
    )
    @DeleteMapping("/{id}")
    public void darDeBaja(@PathVariable Integer id) {
        funcionarioService.darDeBaja(id);
    }

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
    public void reactivar(
            @PathVariable Integer id
    ) {

        funcionarioService.reactivar(id);
    }
}