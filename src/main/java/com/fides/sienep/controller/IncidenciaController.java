package com.fides.sienep.controller;

import com.fides.sienep.dto.IncidenciaRequestDTO;
import com.fides.sienep.dto.IncidenciaResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.IncidenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/incidencias")
@RequiredArgsConstructor
@Tag(name = "Incidencias", description = "Gestión de incidencias")
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    @Operation(summary = "Crear una incidencia")
    @ApiResponse(responseCode = "201", description = "Incidencia creada correctamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Estudiante o funcionario no encontrado")
    @PostMapping
    public ResponseEntity<IncidenciaResponseDTO> crear(@Valid @RequestBody IncidenciaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidenciaService.crear(dto));
    }

    @Operation(summary = "Obtener incidencia por ID")
    @ApiResponse(responseCode = "200", description = "Incidencia encontrada")
    @ApiResponse(responseCode = "404", description = "Incidencia no encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<IncidenciaResponseDTO> obtenerPorId(@PathVariable int id) {
        return ResponseEntity.ok(incidenciaService.obtenerPorId(id));
    }

    @Operation(summary = "Listar incidencias, con filtros opcionales por estudiante y funcionario",
            description = "Los filtros idEstudiante, idFuncionario y estado son opcionales y se combinan entre sí " +
                    "(todos los que se indiquen deben cumplirse). Sin el parámetro estado, devuelve solo las ACTIVAS; " +
                    "admite varios estados separados por coma (estado=ACTIVO,INACTIVO) para listarlas juntas.")
    @ApiResponse(responseCode = "200", description = "Página de incidencias")
    @GetMapping
    public ResponseEntity<Page<IncidenciaResponseDTO>> listar(
            @RequestParam(required = false) Integer idEstudiante,
            @RequestParam(required = false) Integer idFuncionario,
            @Parameter(description = "Estado o estados para filtrar, separados por coma (ej. ACTIVO o ACTIVO,INACTIVO). Por defecto: ACTIVO", required = false)
            @RequestParam(required = false) List<EstadoLogico> estado,
            @ParameterObject @PageableDefault(size = 20, sort = {"fechaHora", "codInstancia"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(incidenciaService.listar(idEstudiante, idFuncionario, estado, pageable));
    }

    @Operation(summary = "Editar una incidencia")
    @ApiResponse(responseCode = "200", description = "Incidencia editada correctamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Incidencia no encontrada")
    @PutMapping("/{id}")
    public ResponseEntity<IncidenciaResponseDTO> editar(@PathVariable int id, @Valid @RequestBody IncidenciaRequestDTO dto) {
        return ResponseEntity.ok(incidenciaService.editar(id, dto));
    }

    @Operation(summary = "Desactivar una incidencia")
    @ApiResponse(responseCode = "204", description = "Incidencia desactivada correctamente")
    @ApiResponse(responseCode = "404", description = "Incidencia no encontrada")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable int id) {
        incidenciaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivar una incidencia")
    @ApiResponse(responseCode = "200", description = "Incidencia reactivada correctamente")
    @ApiResponse(responseCode = "404", description = "Incidencia no encontrada")
    @ApiResponse(responseCode = "409", description = "La incidencia ya está activa")
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<IncidenciaResponseDTO> reactivar(@PathVariable int id) {
        return ResponseEntity.ok(incidenciaService.reactivar(id));
    }

    @Operation(summary = "Ver mis incidencias",
            description = "Permite al estudiante autenticado ver sus propias incidencias activas.")
    @ApiResponse(responseCode = "200", description = "Incidencias obtenidas correctamente")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    @GetMapping("/mis-incidencias")
    public ResponseEntity<Page<IncidenciaResponseDTO>> listarMisIncidencias(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(incidenciaService.listarMisIncidencias(pageable));
    }
}