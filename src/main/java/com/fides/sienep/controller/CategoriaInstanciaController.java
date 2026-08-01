package com.fides.sienep.controller;

import com.fides.sienep.dto.CategoriaInstanciaRequestDTO;
import com.fides.sienep.dto.CategoriaInstanciaResponseDTO;
import com.fides.sienep.dto.CategoriaInstanciaUpdateDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.CategoriaInstanciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias-instancias")
@Tag(name = "Categorías de Instancias", description = "Gestión de categorías que agrupan a las instancias")
@RequiredArgsConstructor
public class CategoriaInstanciaController {

    private final CategoriaInstanciaService categoriaService;

    @Operation(summary = "Listar categorías con filtros", description = "Retorna las categorías. Permite filtrar por coincidencias parciales en el nombre. Sin el parámetro estado, devuelve solo las ACTIVAS.")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @GetMapping
    public ResponseEntity<List<CategoriaInstanciaResponseDTO>> listar(
            @Parameter(description = "Texto opcional para filtrar por nombre", required = false)
            @RequestParam(required = false) String nombre,

            @Parameter(description = "Estado opcional para filtrar (ej. ACTIVO, INACTIVO). Por defecto: ACTIVO", required = false)
            @RequestParam(required = false) EstadoLogico estado) {

        String nombreLimpio = (nombre != null && !nombre.trim().isEmpty()) ? nombre.trim() : null;

        return ResponseEntity.ok(categoriaService.listar(nombreLimpio, estado));
    }

    @Operation(summary = "Obtener una categoría por ID", description = "Busca y retorna los detalles de una categoría específica dado su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoría encontrada"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaInstanciaResponseDTO> obtenerPorId(
            @Parameter(description = "ID de la categoría a buscar", required = true) @PathVariable int id) {
        return ResponseEntity.ok(categoriaService.obtenerPorId(id));
    }

    @Operation(summary = "Crear una nueva categoría de instancia", description = "Registra una nueva categoría en el sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Categoría creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (ej. nombre repetido o faltan campos obligatorios)")
    })
    @PostMapping
    public ResponseEntity<CategoriaInstanciaResponseDTO> crear(
            @Parameter(description = "Objeto con los datos de la nueva categoría", required = true) @Valid @RequestBody CategoriaInstanciaRequestDTO requestDTO) {
        CategoriaInstanciaResponseDTO nueva = categoriaService.crear(requestDTO);
        return new ResponseEntity<>(nueva, HttpStatus.CREATED);
    }

    @Operation(summary = "Editar una categoría existente", description = "Realiza un reemplazo total (PUT) de la categoría. Se deben enviar TODOS los campos (Nombre, Descripción, Estado) en el JSON, incluso si no cambiaron, ya que los campos omitidos podrían ser tratados como nulos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoría editada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaInstanciaResponseDTO> editar(
            @Parameter(description = "ID de la categoría a editar", required = true) @PathVariable int id,
            @Parameter(description = "Objeto con los datos actualizados", required = true) @Valid @RequestBody CategoriaInstanciaUpdateDTO requestDTO) {
        return ResponseEntity.ok(categoriaService.editar(id, requestDTO));
    }

    @Operation(summary = "Desactivar una categoría", description = "Cambia el estado de la categoría a INACTIVO.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Categoría desactivada (No Content)"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(
            @Parameter(description = "ID de la categoría a desactivar", required = true) @PathVariable int id) {
        categoriaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivar una categoría", description = "Vuelve a poner en estado ACTIVO a una categoría inactiva.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoría reactivada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
            @ApiResponse(responseCode = "409", description = "La categoría ya está activa")
    })
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<CategoriaInstanciaResponseDTO> reactivar(@Parameter(description = "ID de la categoría a reactivar", required = true) @PathVariable int id) {
        return ResponseEntity.ok(categoriaService.reactivar(id));
    }
}
