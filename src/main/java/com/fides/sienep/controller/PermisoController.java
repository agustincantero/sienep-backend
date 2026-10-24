package com.fides.sienep.controller;

import com.fides.sienep.dto.PermisoResponseDTO;
import com.fides.sienep.service.PermisoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permisos")
@RequiredArgsConstructor
@Tag(name = "Permisos", description = "Catálogo de permisos del sistema (solo lectura)")
public class PermisoController {

    private final PermisoService permisoService;

    @Operation(summary = "Listar catálogo de permisos", description = "Retorna la lista inmutable de permisos registrados en el sistema.")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @GetMapping
    public ResponseEntity<List<PermisoResponseDTO>> listar() {
        return ResponseEntity.ok(permisoService.listar());
    }
}
