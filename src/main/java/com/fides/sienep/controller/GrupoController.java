package com.fides.sienep.controller;

import com.fides.sienep.dto.GrupoRequestDTO;
import com.fides.sienep.dto.GrupoResponseDTO;
import com.fides.sienep.service.GrupoService;
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
@RequestMapping("/api/grupos")
@RequiredArgsConstructor
@Tag(name = "Grupos", description = "Gestión de grupos académicos")
public class GrupoController {

    private final GrupoService grupoService;


    // CREAR

    @PostMapping
    @Operation(summary = "Crear grupo", description = "Registra un nuevo grupo académico.")
    @ApiResponse(responseCode = "201", description = "Grupo creado")
    @ApiResponse(responseCode = "404", description = "Carrera no encontrada")
    @ApiResponse(responseCode = "409", description = "Ya existe un grupo con ese nombre para esa carrera y generación, o la carrera está inactiva")
    public ResponseEntity<GrupoResponseDTO> crear(
            @Valid @RequestBody GrupoRequestDTO dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(grupoService.crear(dto));
    }


    // OBTENER POR ID

    @GetMapping("/{id}")
    @Operation(summary = "Obtener grupo por ID", description = "Devuelve un grupo específico.")
    @ApiResponse(responseCode = "200", description = "Grupo encontrado")
    @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    public ResponseEntity<GrupoResponseDTO> obtenerPorId(
            @PathVariable Integer id) {

        return ResponseEntity.ok(grupoService.obtenerPorId(id));
    }


    // LISTAR CON FILTROS OPCIONALES

    @GetMapping
    @Operation(
            summary = "Listar grupos activos",
            description = "Devuelve grupos activos. Filtrá por idCarrera y/o generacion para acotar resultados."
    )
    @ApiResponse(responseCode = "200", description = "Listado obtenido")
    public ResponseEntity<List<GrupoResponseDTO>> listar(
            @RequestParam(required = false) Integer idCarrera,
            @RequestParam(required = false) Integer generacion) {

        return ResponseEntity.ok(grupoService.listar(idCarrera, generacion));
    }


    // ACTUALIZAR

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar grupo", description = "Actualiza nombre y generación de un grupo. La carrera no es modificable.")
    @ApiResponse(responseCode = "200", description = "Grupo actualizado")
    @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    @ApiResponse(responseCode = "409", description = "Ya existe un grupo activo con ese nombre para esa carrera y generación")
    public ResponseEntity<GrupoResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody GrupoRequestDTO dto) {

        return ResponseEntity.ok(grupoService.actualizar(id, dto));
    }


    // BAJA LÓGICA

    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja un grupo", description = "Cambia el estado del grupo a INACTIVO.")
    @ApiResponse(responseCode = "204", description = "Grupo dado de baja")
    @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    public ResponseEntity<Void> darDeBaja(
            @PathVariable Integer id) {

        grupoService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }
}