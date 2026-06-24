package com.fides.sienep.controller;

import com.fides.sienep.dto.CategoriaRecordatorioCreateDTO;
import com.fides.sienep.dto.CategoriaRecordatorioResponseDTO;
import com.fides.sienep.dto.CategoriaRecordatorioUpdateDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.CategoriaRecordatorioService;
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
@RequestMapping("/api/categorias-recordatorios")
@Tag(name = "Categorías de Recordatorios", description = "Gestión de categorías de recordatorios")
@RequiredArgsConstructor
public class CategoriaRecordatorioController {

    private final CategoriaRecordatorioService categoriaService;

    @Operation(summary = "Obtener categorías con filtros", description = "Retorna una lista de categorías. Permite filtrar por coincidencias parciales en el nombre y/o por su estado actual.")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @GetMapping
    public ResponseEntity<List<CategoriaRecordatorioResponseDTO>> obtenerTodas(
            @Parameter(description = "Texto opcional para filtrar por nombre", required = false) 
            @RequestParam(required = false) String nombre,
            
            @Parameter(description = "Estado opcional para filtrar (ej. ACTIVO, INACTIVO)", required = false) 
            @RequestParam(required = false) EstadoLogico estado) {

        String nombreLimpio = (nombre != null && !nombre.trim().isEmpty()) ? nombre.trim() : null;
        return ResponseEntity.ok(categoriaService.obtenerTodas(nombreLimpio, estado));
    }

    @Operation(summary = "Obtener una categoría por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoría encontrada"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaRecordatorioResponseDTO> obtenerPorId(
            @Parameter(description = "ID de la categoría a buscar", required = true) @PathVariable int id) {
        return ResponseEntity.ok(categoriaService.obtenerPorId(id));
    }

    @Operation(summary = "Crear una nueva categoría de recordatorio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Categoría creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (ej. nombre repetido)")
    })
    @PostMapping
    public ResponseEntity<CategoriaRecordatorioResponseDTO> crearCategoria(
            @Parameter(description = "Objeto con los datos de la nueva categoría", required = true) @Valid @RequestBody CategoriaRecordatorioCreateDTO requestDTO) {
        CategoriaRecordatorioResponseDTO nueva = categoriaService.crearCategoria(requestDTO);
        return new ResponseEntity<>(nueva, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar una categoría existente", description = "Realiza un reemplazo total (PUT) de la categoría. Se deben enviar TODOS los campos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoría actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaRecordatorioResponseDTO> actualizarCategoria(
            @Parameter(description = "ID de la categoría", required = true) @PathVariable int id,
            @Parameter(description = "Objeto con los datos actualizados", required = true) @Valid @RequestBody CategoriaRecordatorioUpdateDTO requestDTO) {
        return ResponseEntity.ok(categoriaService.actualizarCategoria(id, requestDTO));
    }

    @Operation(summary = "Dar de baja una categoría", description = "Realiza una baja lógica cambiando su estado a INACTIVO.")
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

    @Operation(summary = "Reactivar una categoría", description = "Vuelve a poner en estado ACTIVO a una categoría inactiva.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoría reactivada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
            @ApiResponse(responseCode = "409", description = "La categoría ya está activa")
    })
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<CategoriaRecordatorioResponseDTO> reactivarCategoria(
            @Parameter(description = "ID de la categoría a reactivar", required = true) @PathVariable int id) {
        return ResponseEntity.ok(categoriaService.reactivarCategoria(id));
    }
}
