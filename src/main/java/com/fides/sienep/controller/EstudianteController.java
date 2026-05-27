package com.fides.sienep.controller;

import com.fides.sienep.dto.EstudianteRequestDTO;
import com.fides.sienep.dto.EstudianteResponseDTO;
import com.fides.sienep.dto.EstudianteResumenDTO;
import com.fides.sienep.service.EstudianteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller de Estudiante.
 *
 * Responsabilidad única: recibir la petición HTTP, delegarla al Service
 * y devolver la respuesta con el código de estado correcto.
 * Sin lógica de negocio, sin acceso directo al repositorio.
 *
 * @Valid activa las validaciones definidas en EstudianteRequestDTO
 * antes de que el método se ejecute. Si alguna falla, Spring devuelve
 * 400 Bad Request automáticamente sin llegar al Service.
 */
@RestController
@RequestMapping("/api/v1/estudiantes")
@RequiredArgsConstructor
@Tag(name = "Estudiantes", description = "Gestión de estudiantes del sistema SIENEP")
public class EstudianteController {

    private final EstudianteService estudianteService;

    // =========================================================
    // CREAR
    // =========================================================

    @PostMapping
    @Operation(summary = "Registrar nuevo estudiante",
            description = "Requiere permiso CREAR_ESTUDIANTE. El estudiante queda en estado PENDIENTE_DE_ACTIVACION.")
    @ApiResponse(responseCode = "201", description = "Estudiante creado")
    @ApiResponse(responseCode = "409", description = "Documento o email ya registrado")
    public ResponseEntity<EstudianteResponseDTO> crear(@Valid @RequestBody EstudianteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estudianteService.crear(dto));
    }

    // =========================================================
    // CONSULTAS
    // =========================================================

    @GetMapping("/{id}")
    @Operation(summary = "Obtener ficha completa de un estudiante",
            description = "Requiere permiso VER_ESTUDIANTE.")
    @ApiResponse(responseCode = "200", description = "Estudiante encontrado")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    public ResponseEntity<EstudianteResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(estudianteService.obtenerPorId(id));
    }

    @GetMapping("/{id}/resumen")
    @Operation(summary = "Obtener datos resumidos de un estudiante",
            description = "Requiere permiso VER_ESTUDIANTE_RESUMIDO.")
    public ResponseEntity<EstudianteResumenDTO> obtenerResumenPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(estudianteService.obtenerResumenPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todos los estudiantes con paginación",
            description = "Requiere permiso VER_ESTUDIANTE. Incluye activos e inactivos.")
    public ResponseEntity<Page<EstudianteResponseDTO>> listar(
            @PageableDefault(size = 20, sort = "apellido") Pageable pageable) {
        return ResponseEntity.ok(estudianteService.listar(pageable));
    }

    @GetMapping("/activos")
    @Operation(summary = "Listar estudiantes activos",
            description = "Requiere permiso VER_ESTUDIANTE_RESUMIDO.")
    public ResponseEntity<Page<EstudianteResumenDTO>> listarActivos(
            @PageableDefault(size = 20, sort = "apellido") Pageable pageable) {
        return ResponseEntity.ok(estudianteService.listarActivos(pageable));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar estudiantes por nombre o apellido",
            description = "Requiere permiso BUSCAR_ESTUDIANTE. Búsqueda parcial e insensible a mayúsculas.")
    public ResponseEntity<Page<EstudianteResumenDTO>> buscar(
            @RequestParam String texto,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(estudianteService.buscar(texto, pageable));
    }

    // =========================================================
    // FILTROS
    // =========================================================

    @GetMapping("/filtrar/grupo/{idGrupo}")
    @Operation(summary = "Filtrar estudiantes por grupo",
            description = "Requiere permiso BUSCAR_ESTUDIANTE.")
    public ResponseEntity<Page<EstudianteResumenDTO>> filtrarPorGrupo(
            @PathVariable Integer idGrupo,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(estudianteService.filtrarPorGrupo(idGrupo, pageable));
    }

    @GetMapping("/filtrar/carrera/{idCarrera}")
    @Operation(summary = "Filtrar estudiantes por carrera",
            description = "Requiere permiso BUSCAR_ESTUDIANTE.")
    public ResponseEntity<Page<EstudianteResumenDTO>> filtrarPorCarrera(
            @PathVariable Integer idCarrera,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(estudianteService.filtrarPorCarrera(idCarrera, pageable));
    }

    @GetMapping("/filtrar/itr/{idItr}")
    @Operation(summary = "Filtrar estudiantes por ITR",
            description = "Requiere permiso BUSCAR_ESTUDIANTE.")
    public ResponseEntity<Page<EstudianteResumenDTO>> filtrarPorItr(
            @PathVariable Integer idItr,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(estudianteService.filtrarPorItr(idItr, pageable));
    }

    // =========================================================
    // ACTUALIZAR
    // =========================================================

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos de un estudiante",
            description = "Requiere permiso EDITAR_ESTUDIANTE.")
    @ApiResponse(responseCode = "200", description = "Estudiante actualizado")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    public ResponseEntity<EstudianteResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody EstudianteRequestDTO dto) {
        return ResponseEntity.ok(estudianteService.actualizar(id, dto));
    }

    @PutMapping("/{id}/mis-datos")
    @Operation(summary = "El estudiante edita sus propios datos",
            description = "Requiere permiso EDITAR_MIS_DATOS. Solo puede editar los datos propios.")
    public ResponseEntity<EstudianteResponseDTO> editarMisDatos(
            @PathVariable Integer id,
            @Valid @RequestBody EstudianteRequestDTO dto) {
        return ResponseEntity.ok(estudianteService.editarMisDatos(id, dto));
    }

    // =========================================================
    // CONTRASEÑA — ACTIVA EL USUARIO
    // =========================================================

    @PatchMapping("/{id}/contrasenia")
    @Operation(summary = "Cambiar contraseña",
            description = "Disponible para cualquier usuario autenticado sobre su propia cuenta. " +
                    "Si el estudiante estaba PENDIENTE_DE_ACTIVACION, pasa a ACTIVO.")
    @ApiResponse(responseCode = "204", description = "Contraseña actualizada")
    public ResponseEntity<Void> cambiarContrasenia(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        estudianteService.cambiarContrasenia(id, body.get("contrasenia"));
        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // BAJA Y REACTIVACIÓN
    // =========================================================

    @PatchMapping("/{id}/baja")
    @Operation(summary = "Dar de baja lógica a un estudiante",
            description = "Requiere permiso ELIMINAR_ESTUDIANTE. No borra datos — cambia estado a INACTIVO.")
    @ApiResponse(responseCode = "204", description = "Estudiante dado de baja")
    public ResponseEntity<Void> darDeBaja(@PathVariable Integer id) {
        estudianteService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivar")
    @Operation(summary = "Reactivar un estudiante dado de baja",
            description = "Requiere permiso REACTIVAR_ESTUDIANTE.")
    @ApiResponse(responseCode = "204", description = "Estudiante reactivado")
    public ResponseEntity<Void> reactivar(@PathVariable Integer id) {
        estudianteService.reactivar(id);
        return ResponseEntity.noContent().build();
    }
}