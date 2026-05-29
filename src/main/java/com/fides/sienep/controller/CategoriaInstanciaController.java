package com.fides.sienep.controller;

import com.fides.sienep.dto.CategoriaInstanciaCreateDTO;
import com.fides.sienep.dto.CategoriaInstanciaResponseDTO;
import com.fides.sienep.dto.CategoriaInstanciaUpdateDTO;
import com.fides.sienep.service.CategoriaInstanciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias-instancias")
@Tag(name = "Categorías de Instancias Comunes", description = "CRUD para la gestión de categorías que agrupan a las instancias comunes de seguimiento psicopedagógico")
public class CategoriaInstanciaController {

    private final CategoriaInstanciaService categoriaService;

    @Autowired
    public CategoriaInstanciaController(CategoriaInstanciaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @Operation(summary = "Obtener todas las categorías", description = "Retorna una lista con todas las categorías de instancias.")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @GetMapping
    public ResponseEntity<List<CategoriaInstanciaResponseDTO>> obtenerTodas() {
        return ResponseEntity.ok(categoriaService.obtenerTodas());
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
    public ResponseEntity<CategoriaInstanciaResponseDTO> crearCategoria(
            @Parameter(description = "Objeto con los datos de la nueva categoría", required = true) @Valid @RequestBody CategoriaInstanciaCreateDTO requestDTO) {
        CategoriaInstanciaResponseDTO nueva = categoriaService.crearCategoria(requestDTO);
        return new ResponseEntity<>(nueva, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar una categoría existente", description = "Realiza un reemplazo total (PUT) de la categoría. Se deben enviar TODOS los campos (Nombre, Descripción, Estado) en el JSON, incluso si no cambiaron, ya que los campos omitidos podrían ser tratados como nulos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoría actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaInstanciaResponseDTO> actualizarCategoria(
            @Parameter(description = "ID de la categoría a actualizar", required = true) @PathVariable int id,
            @Parameter(description = "Objeto con los datos actualizados", required = true) @Valid @RequestBody CategoriaInstanciaUpdateDTO requestDTO) {
        return ResponseEntity.ok(categoriaService.actualizarCategoria(id, requestDTO));
    }

    @Operation(summary = "Dar de baja una categoría", description = "Realiza una baja lógica de la categoría cambiando su estado a INACTIVO.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Baja exitosa (No Content)"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBajaCategoria(
            @Parameter(description = "ID de la categoría a dar de baja", required = true) @PathVariable int id) {
        categoriaService.darDeBajaCategoria(id);
        return ResponseEntity.noContent().build();
    }
}
