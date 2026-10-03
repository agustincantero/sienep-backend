package com.fides.sienep.controller;

import com.fides.sienep.dto.InstanciaComunRequestDTO;
import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.dto.InstanciaComunUpdateDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.InstanciaComunService;
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

@RestController
@RequestMapping("/instancias")
@RequiredArgsConstructor
@Tag(name = "Instancias", description = "Gestión de instancias formales (reuniones, llamadas, seguimientos)")
public class InstanciaComunController {

    private final InstanciaComunService instanciaComunService;

    @Operation(summary = "Crear una instancia",
            description = "Crea una instancia asociada a un estudiante (RF13, sincronizada con Google Calendar). " +
                    "Opcionalmente, se puede incluir el campo 'recordatorioInicial' para crear en la misma " +
                    "operación un Recordatorio vinculado a la instancia recién creada — best-effort: si falla " +
                    "(categoría inválida, sin permiso, etc.) la instancia se crea igual, sin el recordatorio. " +
                    "Con Google Calendar activo (RF22), el recordatorio se sincroniza automáticamente en el " +
                    "calendario compartido de la organización.")
    @ApiResponse(responseCode = "201", description = "Instancia creada correctamente (y recordatorio, si se solicitó)")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Estudiante, funcionario, categoría, frecuencia o responsable de recordatorio no encontrado")
    @ApiResponse(responseCode = "422", description = "Estudiante, funcionario o categoría no activos")
    @PostMapping
    public ResponseEntity<InstanciaComunResponseDTO> crear(
            @Valid @RequestBody InstanciaComunRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(instanciaComunService.crear(dto));
    }

    @Operation(summary = "Obtener instancia por ID")
    @ApiResponse(responseCode = "200", description = "Instancia encontrada")
    @ApiResponse(responseCode = "404", description = "Instancia no encontrada")
    @GetMapping("/{codInstancia}")
    public ResponseEntity<InstanciaComunResponseDTO> obtenerPorId(@PathVariable int codInstancia) {
        return ResponseEntity.ok(instanciaComunService.obtenerPorId(codInstancia));
    }

    @Operation(summary = "Listar instancias con filtros opcionales",
            description = "Los filtros idEstudiante, idFuncionario, idCategoria y estado son opcionales y se combinan " +
                    "entre sí (todos los que se indiquen deben cumplirse). Sin el parámetro estado, devuelve solo las ACTIVAS.")
    @ApiResponse(responseCode = "200", description = "Página de instancias")
    @GetMapping
    public ResponseEntity<Page<InstanciaComunResponseDTO>> listar(
            @RequestParam(required = false) Integer idEstudiante,
            @RequestParam(required = false) Integer idFuncionario,
            @RequestParam(required = false) Integer idCategoria,
            @Parameter(description = "Estado opcional para filtrar (ej. ACTIVO, INACTIVO). Por defecto: ACTIVO", required = false)
            @RequestParam(required = false) EstadoLogico estado,
            @ParameterObject @PageableDefault(size = 20, sort = {"fechaHora", "codInstancia"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                instanciaComunService.listar(idEstudiante, idFuncionario, idCategoria, estado, pageable));
    }

    @Operation(summary = "Editar una instancia")
    @ApiResponse(responseCode = "200", description = "Instancia editada correctamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "Instancia o categoría no encontrada")
    @PutMapping("/{codInstancia}")
    public ResponseEntity<InstanciaComunResponseDTO> editar(
            @PathVariable int codInstancia,
            @Valid @RequestBody InstanciaComunUpdateDTO dto) {
        return ResponseEntity.ok(instanciaComunService.editar(codInstancia, dto));
    }

    @Operation(summary = "Desactivar una instancia")
    @ApiResponse(responseCode = "204", description = "Instancia desactivada correctamente")
    @ApiResponse(responseCode = "404", description = "Instancia no encontrada")
    @DeleteMapping("/{codInstancia}")
    public ResponseEntity<Void> desactivar(@PathVariable int codInstancia) {
        instanciaComunService.desactivar(codInstancia);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reactivar una instancia")
    @ApiResponse(responseCode = "200", description = "Instancia reactivada correctamente")
    @ApiResponse(responseCode = "404", description = "Instancia no encontrada")
    @ApiResponse(responseCode = "409", description = "La instancia ya está activa")
    @PatchMapping("/{codInstancia}/reactivar")
    public ResponseEntity<InstanciaComunResponseDTO> reactivar(@PathVariable int codInstancia) {
        return ResponseEntity.ok(instanciaComunService.reactivar(codInstancia));
    }

    @Operation(summary = "Ver mis instancias",
            description = "Permite al estudiante autenticado ver sus propias instancias activas.")
    @ApiResponse(responseCode = "200", description = "Instancias obtenidas correctamente")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    @GetMapping("/mis-instancias")
    public ResponseEntity<Page<InstanciaComunResponseDTO>> listarMisInstancias(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(instanciaComunService.listarMisInstancias(pageable));
    }
}