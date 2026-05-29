package com.fides.sienep.controller;

import com.fides.sienep.dto.PermisoUpdateDTO;
import com.fides.sienep.model.Permiso;
import com.fides.sienep.service.PermisoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permisos")
@Tag(name = "Permisos", description = "Catálogo de permisos del sistema (Solo lectura y modificación de descripciones)")
public class PermisoController {

    private final PermisoService permisoService;

    @Autowired
    public PermisoController(PermisoService permisoService) {
        this.permisoService = permisoService;
    }

    @Operation(summary = "Obtener catálogo de permisos", description = "Retorna la lista inmutable de permisos registrados en el sistema.")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @GetMapping
    public ResponseEntity<List<Permiso>> obtenerTodos() {
        return ResponseEntity.ok(permisoService.obtenerTodos());
    }

    @Operation(summary = "Actualizar descripción del permiso", description = "Permite modificar únicamente la descripción del permiso para mayor claridad. El nombre del permiso y su estado lógico son inmutables.")
    @ApiResponse(responseCode = "200", description = "Descripción actualizada exitosamente")
    @PutMapping("/{id}")
    public ResponseEntity<Permiso> actualizarDescripcion(
            @Parameter(description = "ID del permiso a actualizar", required = true) @PathVariable int id,
            @Parameter(description = "Objeto con la nueva descripción", required = true) @Valid @RequestBody PermisoUpdateDTO requestDTO) {
        return ResponseEntity.ok(permisoService.actualizarDescripcion(id, requestDTO));
    }
}
