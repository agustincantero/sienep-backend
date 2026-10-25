package com.fides.sienep.controller;

import com.fides.sienep.dto.AuditoriaResponseDTO;
import com.fides.sienep.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fides.sienep.model.TipoAccion;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/auditorias")
@RequiredArgsConstructor
@Tag(name = "Auditoría", description = "Endpoints para la consulta y trazabilidad de eventos del sistema")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @Operation(summary = "Listar registros de auditoría", description = "Obtiene el historial completo de eventos ocurridos en el sistema ordenados del más reciente al más antiguo con paginación y filtros opcionales.")
    @ApiResponse(responseCode = "200", description = "Lista de registros de auditoría obtenida exitosamente")
    @GetMapping
    public ResponseEntity<Page<AuditoriaResponseDTO>> listarAuditorias(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) TipoAccion accion,
            @RequestParam(required = false) Integer idAutor,
            @RequestParam(required = false) String texto) {

        return ResponseEntity.ok(auditoriaService.listarAuditorias(page, size, entidad, accion, idAutor, texto));
    }
}
