package com.fides.sienep.controller;

import com.fides.sienep.dto.FuncionarioRequestDTO;
import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.service.FuncionarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funcionarios")
public class FuncionarioController {

    @Autowired
    private FuncionarioService funcionarioService;

    // LISTAR TODOS

    @GetMapping
    public List<FuncionarioResponseDTO> listar() {

        return funcionarioService.listar();
    }


    // OBTENER POR ID


    @GetMapping("/{id}")
    public FuncionarioResponseDTO obtener(
            @PathVariable Integer id
    ) {

        return funcionarioService.obtenerPorId(id);
    }


    // CREAR


    @PostMapping
    public FuncionarioResponseDTO crear(
            @RequestBody FuncionarioRequestDTO dto
    ) {

        return funcionarioService.crear(dto);
    }


    // ELIMINAR


    @DeleteMapping("/{id}")
    public void eliminar(
            @PathVariable Integer id
    ) {

        funcionarioService.eliminar(id);
    }
}