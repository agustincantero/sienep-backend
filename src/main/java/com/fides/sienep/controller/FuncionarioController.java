package com.fides.sienep.controller;

import com.fides.sienep.dto.FuncionarioRequestDTO;
import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.service.FuncionarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/funcionarios")
@RequiredArgsConstructor
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    @PostMapping
    public ResponseEntity<FuncionarioResponseDTO> crear(
            @Valid @RequestBody FuncionarioRequestDTO dto
    ) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(funcionarioService.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuncionarioResponseDTO> obtenerPorId(
            @PathVariable Integer id
    ) {

        return ResponseEntity.ok(
                funcionarioService.obtenerPorId(id)
        );
    }
}