package com.fides.sienep.controller;

import com.fides.sienep.dto.AtenderSolicitudInstanciaDTO;
import com.fides.sienep.dto.RechazarSolicitudInstanciaDTO;
import com.fides.sienep.dto.SolicitudInstanciaRequestDTO;
import com.fides.sienep.dto.SolicitudInstanciaResponseDTO;
import com.fides.sienep.model.EstadoSolicitud;
import com.fides.sienep.service.SolicitudInstanciaService;
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
@RequestMapping("/solicitudes-instancia")
@RequiredArgsConstructor
@Tag(name = "Solicitudes de instancia", description = "Pedidos de instancia hechos por los estudiantes")
public class SolicitudInstanciaController {

    private final SolicitudInstanciaService solicitudService;

    @PostMapping
    @Operation(summary = "Solicitar una instancia",
            description = "Solo el estudiante autenticado, para sí mismo. Queda PENDIENTE hasta que un funcionario " +
                    "la atienda o la rechace. Máximo 3 pendientes por estudiante.")
    @ApiResponse(responseCode = "201", description = "Solicitud creada")
    @ApiResponse(responseCode = "409", description = "Ya tiene el máximo de solicitudes pendientes")
    @ApiResponse(responseCode = "422", description = "La cuenta del estudiante no está activa")
    public ResponseEntity<SolicitudInstanciaResponseDTO> crear(@Valid @RequestBody SolicitudInstanciaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.crear(dto));
    }

    @GetMapping("/mias")
    @Operation(summary = "Ver mis solicitudes", description = "Las solicitudes del estudiante autenticado, más recientes primero.")
    @ApiResponse(responseCode = "200", description = "Página de solicitudes")
    public ResponseEntity<Page<SolicitudInstanciaResponseDTO>> listarMias(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(solicitudService.listarMias(pageable));
    }

    @GetMapping
    @Operation(summary = "Listar solicitudes", description = "Requiere VER_SOLICITUDES_INSTANCIA. Más recientes primero.")
    @ApiResponse(responseCode = "200", description = "Página de solicitudes")
    public ResponseEntity<Page<SolicitudInstanciaResponseDTO>> listar(
            @Parameter(description = "Filtro opcional por estado (ej. PENDIENTE). Sin filtro: todas.")
            @RequestParam(required = false) EstadoSolicitud estado,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(solicitudService.listar(estado, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver una solicitud", description = "Requiere VER_SOLICITUDES_INSTANCIA, o ser el estudiante que la hizo.")
    @ApiResponse(responseCode = "200", description = "Solicitud encontrada")
    @ApiResponse(responseCode = "403", description = "Es de otro estudiante")
    @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    public ResponseEntity<SolicitudInstanciaResponseDTO> obtenerPorId(@PathVariable int id) {
        return ResponseEntity.ok(solicitudService.obtenerPorId(id));
    }

    // Mismo criterio que POST /recordatorios/{id}/instancias: acción de negocio con efecto secundario (crea
    // una instancia), nombrada como sub-recurso para no meter un verbo en la URL.
    @PostMapping("/{id}/instancias")
    @Operation(summary = "Atender una solicitud",
            description = "Requiere ATENDER_SOLICITUD_INSTANCIA y CREAR_INSTANCIA. Crea la instancia para el estudiante " +
                    "de la solicitud (el funcionario autenticado queda como responsable) y la marca ATENDIDA.")
    @ApiResponse(responseCode = "201", description = "Instancia creada y solicitud atendida")
    @ApiResponse(responseCode = "404", description = "Solicitud o categoría no encontrada")
    @ApiResponse(responseCode = "409", description = "La solicitud ya fue resuelta")
    @ApiResponse(responseCode = "422", description = "El estudiante o la categoría no están activos")
    public ResponseEntity<SolicitudInstanciaResponseDTO> atender(
            @PathVariable int id,
            @Valid @RequestBody AtenderSolicitudInstanciaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.atender(id, dto));
    }

    @PatchMapping("/{id}/rechazar")
    @Operation(summary = "Rechazar una solicitud",
            description = "Requiere ATENDER_SOLICITUD_INSTANCIA. La respuesta es obligatoria: es lo que ve el estudiante.")
    @ApiResponse(responseCode = "200", description = "Solicitud rechazada")
    @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    @ApiResponse(responseCode = "409", description = "La solicitud ya fue resuelta")
    public ResponseEntity<SolicitudInstanciaResponseDTO> rechazar(
            @PathVariable int id,
            @Valid @RequestBody RechazarSolicitudInstanciaDTO dto) {
        return ResponseEntity.ok(solicitudService.rechazar(id, dto));
    }
}
