package com.fides.sienep.controller;

import com.fides.sienep.dto.AuditoriaResponseDTO;
import com.fides.sienep.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.fides.sienep.model.TipoAccion;
import org.springframework.data.domain.Page;

import java.util.List;

@RestController
@RequestMapping("/api/auditorias")
@RequiredArgsConstructor
@Tag(name = "Auditoría", description = "Endpoints para la consulta y trazabilidad de eventos del sistema")
@PreAuthorize("hasAuthority('VER_AUDITORIA')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @Operation(summary = "Listar registros de auditoría", description = "Obtiene el historial completo de eventos ocurridos en el sistema ordenados del más reciente al más antiguo con paginación y filtros opcionales.")
    @GetMapping
    public ResponseEntity<Page<AuditoriaResponseDTO>> listarAuditorias(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) TipoAccion accion,
            @RequestParam(required = false) Integer idAutor) {
        
        return ResponseEntity.ok(auditoriaService.listarAuditorias(page, size, entidad, accion, idAutor));
    }

    @Operation(summary = "Ver detalle de auditoría", description = "Obtiene los detalles específicos de un registro de auditoría mediante su ID, incluyendo los valores previos y nuevos en formato JSON.")
    @GetMapping("/{id}")
    public ResponseEntity<AuditoriaResponseDTO> obtenerAuditoriaPorId(@PathVariable int id) {
        return ResponseEntity.ok(auditoriaService.obtenerAuditoriaPorId(id));
    }
}
