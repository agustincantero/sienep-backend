package com.fides.sienep.controller;

import com.fides.sienep.dto.RolCreateDTO;
import com.fides.sienep.dto.RolUpdateDTO;
import com.fides.sienep.dto.RolResponseDTO;
import com.fides.sienep.service.RolService;
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
@RequestMapping("/api/roles")
@Tag(name = "Roles", description = "Gestión de roles y sus permisos")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    @Operation(summary = "Obtener todos los roles", description = "Retorna una lista con todos los roles registrados en el sistema, incluyendo sus permisos activos.")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @GetMapping
    public ResponseEntity<List<RolResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(rolService.obtenerTodos());
    }

    @Operation(summary = "Obtener un rol por ID", description = "Busca y retorna los detalles de un rol específico dado su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol encontrado"),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<RolResponseDTO> obtenerPorId(
            @Parameter(description = "ID del rol a buscar", required = true) @PathVariable int id) {
        return ResponseEntity.ok(rolService.obtenerPorId(id));
    }

    @Operation(summary = "Crear un nuevo rol", description = "Registra un nuevo rol en el sistema y le asigna los permisos indicados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rol creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (ej. nombre repetido o faltan campos obligatorios)")
    })
    @PostMapping
    public ResponseEntity<RolResponseDTO> crearRol(
            @Parameter(description = "Objeto con los datos del nuevo rol", required = true) @Valid @RequestBody RolCreateDTO requestDTO) {
        RolResponseDTO nuevoRol = rolService.crearRol(requestDTO);
        return new ResponseEntity<>(nuevoRol, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar un rol existente", description = "Modifica los datos de un rol y actualiza su lista de permisos (realizando altas y bajas lógicas según corresponda). Si se envía una lista vacía de permisos, se eliminan todos los permisos asociados al rol.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<RolResponseDTO> actualizarRol(
            @Parameter(description = "ID del rol a actualizar", required = true) @PathVariable int id,
            @Parameter(description = "Objeto con los datos actualizados", required = true) @Valid @RequestBody RolUpdateDTO requestDTO) {
        return ResponseEntity.ok(rolService.actualizarRol(id, requestDTO));
    }

    @Operation(summary = "Desactivar un rol", description = "Realiza una baja lógica del rol cambiando su estado a INACTIVO.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Rol dado de baja exitosamente (No Content)"),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivarRol(
            @Parameter(description = "ID del rol a desactivar", required = true) @PathVariable int id) {
        rolService.desactivarRol(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivar un rol", description = "Vuelve a poner en estado ACTIVO a un rol inactivo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol reactivado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado"),
            @ApiResponse(responseCode = "409", description = "El rol ya está activo")
    })
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<RolResponseDTO> reactivarRol(
            @Parameter(description = "ID del rol a reactivar", required = true) @PathVariable int id) {
        return ResponseEntity.ok(rolService.reactivarRol(id));
    }
}
