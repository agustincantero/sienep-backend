package com.fides.sienep.controller;

import com.fides.sienep.dto.TelefonoRequestDTO;
import com.fides.sienep.dto.TelefonoResponseDTO;
import com.fides.sienep.service.TelefonoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/telefonos")
@RequiredArgsConstructor
@Tag(name = "Teléfonos", description = "Gestión de teléfonos de estudiantes")
public class TelefonoController {

    private final TelefonoService telefonoService;

    @Operation(summary = "Listar teléfonos de un estudiante")
    @ApiResponse(responseCode = "200", description = "Lista de teléfonos")
    @GetMapping
    public ResponseEntity<List<TelefonoResponseDTO>> listar(
            @RequestParam Integer idUsuario) {
        return ResponseEntity.ok(telefonoService.listarPorEstudiante(idUsuario));
    }

    @Operation(summary = "Agregar teléfono a un estudiante")
    @ApiResponse(responseCode = "201", description = "Teléfono creado")
    @PostMapping
    public ResponseEntity<TelefonoResponseDTO> agregar(
            @RequestBody @Valid TelefonoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(telefonoService.agregar(dto));
    }

    @Operation(summary = "Baja lógica de un teléfono")
    @ApiResponse(responseCode = "204", description = "Teléfono eliminado")
    @DeleteMapping
    public ResponseEntity<Void> eliminar(
            @RequestParam Integer idUsuario,
            @RequestParam String numero) {
        telefonoService.eliminar(idUsuario, numero);
        return ResponseEntity.noContent().build();
    }
}
