package com.fides.sienep.controller;

import com.fides.sienep.dto.*;
import com.fides.sienep.model.EstadoLogico;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
@Tag(name = "Estudiantes", description = "Gestión de estudiantes del sistema SIENEP")
public class EstudianteController {

    private final EstudianteService estudianteService;

    // =========================================================
    // CREAR
    // =========================================================

    @PostMapping
    @Operation(summary = "Registrar nuevo estudiante",
            description = "Requiere CREAR_ESTUDIANTE. Estado inicial: PENDIENTE_DE_ACTIVACION.")
    @ApiResponse(responseCode = "201", description = "Estudiante creado")
    @ApiResponse(responseCode = "409", description = "Documento o email ya registrado")
    public ResponseEntity<EstudianteResponseDTO> crear(@Valid @RequestBody EstudianteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estudianteService.crear(dto));
    }

    // =========================================================
    // CONSULTAR COLECCIÓN
    // =========================================================

    /**
     * Endpoint único para listar, buscar y filtrar estudiantes.
     *
     * Todos los parámetros son opcionales y combinables:
     *   ?texto=         busca por nombre o apellido parcial     requiere BUSCAR_ESTUDIANTE
     *   ?grupo=         filtra por idGrupo                      requiere BUSCAR_ESTUDIANTE
     *   ?carrera=       filtra por idCarrera                    requiere BUSCAR_ESTUDIANTE
     *   ?itr=           filtra por idItr                        requiere BUSCAR_ESTUDIANTE
     *   ?estado=        filtra por estado (ACTIVO/INACTIVO)     solo roles con VER_ESTUDIANTE ven inactivos
     *   (sin parámetros) lista activos                          requiere VER_ESTUDIANTE_RESUMIDO
     *
     * Siempre devuelve el DTO resumido. Los datos completos se obtienen
     * por GET /estudiantes/{id} para evitar volcar datos de salud en masa.
     */
    @GetMapping
    @Operation(summary = "Listar, buscar y filtrar estudiantes",
            description = "Sin parámetros: lista activos. Con texto/grupo/carrera/itr: requiere BUSCAR_ESTUDIANTE. " +
                    "Roles sin VER_ESTUDIANTE solo ven activos aunque envíen ?estado=INACTIVO.")
    @ApiResponse(responseCode = "200", description = "Lista paginada de estudiantes")
    @ApiResponse(responseCode = "403", description = "Permiso insuficiente para el filtro solicitado")
    public ResponseEntity<Page<EstudianteResumenDTO>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Integer grupo,
            @RequestParam(required = false) Integer carrera,
            @RequestParam(required = false) Integer itr,
            @RequestParam(required = false) EstadoLogico estado,
            @PageableDefault(size = 20, sort = "apellido") Pageable pageable) {
        return ResponseEntity.ok(estudianteService.listar(texto, grupo, carrera, itr, estado, pageable));
    }

    // =========================================================
    // CONSULTAR INDIVIDUAL
    // =========================================================

    @GetMapping("/{id}")
    @Operation(summary = "Obtener ficha completa de un estudiante",
            description = "Requiere VER_ESTUDIANTE. Incluye datos de salud y motivo de derivación.")
    @ApiResponse(responseCode = "200", description = "Estudiante encontrado")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    public ResponseEntity<EstudianteResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(estudianteService.obtenerPorId(id));
    }

    @GetMapping("/{id}/resumen")
    @Operation(summary = "Obtener datos resumidos de un estudiante",
            description = "Requiere VER_ESTUDIANTE_RESUMIDO. Sin datos de salud.")
    @ApiResponse(responseCode = "200", description = "Estudiante encontrado")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    public ResponseEntity<EstudianteResumenDTO> obtenerResumenPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(estudianteService.obtenerResumenPorId(id));
    }

    // =========================================================
    // ACTUALIZAR
    // =========================================================

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos de un estudiante",
            description = "Requiere EDITAR_ESTUDIANTE.")
    @ApiResponse(responseCode = "200", description = "Estudiante actualizado")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    public ResponseEntity<EstudianteResponseDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody EstudianteUpdateDTO dto) {
        return ResponseEntity.ok(estudianteService.actualizar(id, dto));
    }

    @PutMapping("/{id}/mis-datos")
    @Operation(summary = "El estudiante edita sus propios datos de contacto",
            description = "Solo para el estudiante autenticado (ROLE_ESTUDIANTE) sobre su propia cuenta. " +
                    "Permite editar dirección y teléfonos únicamente.")
    @ApiResponse(responseCode = "200", description = "Datos actualizados")
    @ApiResponse(responseCode = "403", description = "Intento de editar datos de otra cuenta")
    public ResponseEntity<EstudianteResponseDTO> editarMisDatos(
            @PathVariable Integer id,
            @Valid @RequestBody MisDatosRequestDTO dto) {
        return ResponseEntity.ok(estudianteService.editarMisDatos(id, dto));
    }

    @Operation(summary = "Ver mi perfil",
            description = "Permite al estudiante autenticado ver su propio perfil resumido.")
    @ApiResponse(responseCode = "200", description = "Perfil obtenido correctamente")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    @GetMapping("/mi-perfil")
    public ResponseEntity<EstudianteResumenDTO> verMiPerfil() {
        return ResponseEntity.ok(estudianteService.verMiPerfil());
    }

    // =========================================================
    // CONTRASEÑA
    // =========================================================

    @PatchMapping("/{id}/contrasenia")
    @Operation(summary = "Cambiar contraseña",
            description = "Cualquier usuario autenticado sobre su propia cuenta. " +
                    "Si el estudiante estaba PENDIENTE_DE_ACTIVACION, pasa a ACTIVO.")
    @ApiResponse(responseCode = "204", description = "Contraseña actualizada")
    @ApiResponse(responseCode = "403", description = "Intento de cambiar contraseña de otro usuario")
    public ResponseEntity<Void> cambiarContrasenia(
            @PathVariable Integer id,
            @Valid @RequestBody CambiarContraseniaDTO dto) {
        estudianteService.cambiarContrasenia(id, dto);
        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // BAJA Y REACTIVACIÓN
    // =========================================================

    /**
     * Baja lógica del estudiante.
     * DELETE semánticamente = sacar de la nómina activa.
     * Internamente cambia estado a INACTIVO — no borra datos.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja lógica a un estudiante",
            description = "Requiere ELIMINAR_ESTUDIANTE. No elimina datos — estado pasa a INACTIVO.")
    @ApiResponse(responseCode = "204", description = "Estudiante dado de baja")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    public ResponseEntity<Void> darDeBaja(@PathVariable Integer id) {
        estudianteService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivación de un estudiante dado de baja.
     * PATCH sobre el sub-recurso /estado — modifica parcialmente el estado del recurso.
     * El DTO debe traer estado = ACTIVO. Si trae otro valor, se rechaza con 400.
     */
    @PatchMapping("/{id}/reactivar")
    @Operation(summary = "Reactivar un estudiante dado de baja",
            description = "Requiere REACTIVAR_ESTUDIANTE. Sin body — la URL define la acción.")
    @ApiResponse(responseCode = "204", description = "Estudiante reactivado")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    public ResponseEntity<Void> reactivar(@PathVariable Integer id) {
        estudianteService.reactivar(id);
        return ResponseEntity.noContent().build();
    }
}