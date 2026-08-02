package com.fides.sienep.controller;

import com.fides.sienep.dto.CambiarContraseniaDTO;
import com.fides.sienep.dto.FuncionarioRequestDTO;
import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.dto.FuncionarioUpdateDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.FuncionarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/funcionarios")
@RequiredArgsConstructor
@Tag(name = "Funcionarios", description = "Gestión de funcionarios")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    @Operation(summary = "Listar funcionarios", description = "Sin el parámetro estado, devuelve solo los ACTIVOS.")
    @ApiResponse(responseCode = "200", description = "Funcionarios obtenidos correctamente")
    @GetMapping
    public ResponseEntity<Page<FuncionarioResponseDTO>> listar(
            @Parameter(description = "Estado opcional para filtrar (ej. ACTIVO, INACTIVO). Por defecto: ACTIVO", required = false)
            @RequestParam(required = false) EstadoLogico estado,
            @ParameterObject @PageableDefault(size = 20, sort = "apellido") Pageable pageable) {

        return ResponseEntity.ok(funcionarioService.listar(estado, pageable));
    }

    @Operation(summary = "Obtener funcionario por ID")
    @ApiResponse(responseCode = "200", description = "Funcionario encontrado")
    @ApiResponse(responseCode = "404", description = "Funcionario no encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<FuncionarioResponseDTO> obtenerPorId(@PathVariable Integer id) {

        // El servicio ya lanza 404 si no existe.
        // El controller solo envuelve el resultado exitoso en 200.
        return ResponseEntity.ok(funcionarioService.obtenerPorId(id));
    }

    @Operation(summary = "Crear funcionario")
    @ApiResponse(responseCode = "201", description = "Funcionario creado correctamente")
    @PostMapping
    public ResponseEntity<FuncionarioResponseDTO> crear(@Valid @RequestBody FuncionarioRequestDTO dto) {

        FuncionarioResponseDTO creado = funcionarioService.crear(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(summary = "Editar funcionario")
    @ApiResponse(responseCode = "200", description = "Funcionario editado correctamente")
    @ApiResponse(responseCode = "404", description = "Funcionario no encontrado")
    @PutMapping("/{id}")
    public ResponseEntity<FuncionarioResponseDTO> editar(
            @PathVariable Integer id,
            @Valid @RequestBody FuncionarioUpdateDTO dto) {

        return ResponseEntity.ok(funcionarioService.editar(id, dto));
    }

    @Operation(summary = "Desactivar funcionario")
    @ApiResponse(responseCode = "204", description = "Funcionario desactivado correctamente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        funcionarioService.desactivar(id);

        // 204 No Content: la operación fue exitosa pero no hay nada que devolver en el body (ya no existe el recurso).
        return ResponseEntity.noContent().build();
    }

    // ========================================================
    //SOLE
    @Operation(summary = "Reactivar funcionario")
    @ApiResponse(
            responseCode = "200",
            description = "Funcionario reactivado correctamente"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Funcionario no encontrado"
    )
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<FuncionarioResponseDTO> reactivar(@PathVariable Integer id) {
        return ResponseEntity.ok(funcionarioService.reactivar(id));
    }



    @Operation(summary = "Cambiar contraseña propia",
            description = "Exclusivo del propio funcionario autenticado — requiere la contraseña actual. " +
                    "Si estaba PENDIENTE_DE_ACTIVACION pasa a ACTIVO. Para restablecerle la contraseña " +
                    "a otro funcionario, usar POST /{id}/contrasenia en su lugar.")
    @ApiResponse(responseCode = "204", description = "Contraseña actualizada")
    @ApiResponse(responseCode = "401", description = "La contraseña actual no es correcta")
    @ApiResponse(responseCode = "403", description = "Intento de cambiar contraseña de otro usuario")
    @PatchMapping("/{id}/contrasenia")
    public ResponseEntity<Void> cambiarContrasenia(
            @PathVariable Integer id,
            @Valid @RequestBody CambiarContraseniaDTO dto) {
        funcionarioService.cambiarContrasenia(id, dto);
        return ResponseEntity.noContent().build();
    }

    // Mismo sub-recurso que cambiarContrasenia (/{id}/contrasenia): acá el cliente no manda
    // ningún body porque no es él quien define el valor nuevo, lo genera el servidor y lo
    // manda por email. POST (no PATCH) porque es una acción con efecto secundario, no una
    // modificación de estado descrita por el cliente — mismo criterio que se usó en RF27
    // para no meter un verbo en la URL (ver crearInstanciaDesdeRecordatorio).
    @Operation(summary = "Restablecer contraseña de un funcionario",
            description = "Requiere EDITAR_FUNCIONARIO. Genera una nueva contraseña y la envía por email.")
    @ApiResponse(responseCode = "204", description = "Contraseña restablecida y email enviado")
    @ApiResponse(responseCode = "404", description = "Funcionario no encontrado")
    @PostMapping("/{id}/contrasenia")
    public ResponseEntity<Void> restablecerContrasenia(@PathVariable Integer id) {
        funcionarioService.restablecerContrasenia(id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================


    @Operation(summary = "Obtener funcionario por documento")
    @ApiResponse(responseCode = "200", description = "Funcionario encontrado")
    @ApiResponse(responseCode = "404", description = "Funcionario no encontrado")
    @GetMapping("/documento/{documento}")
    public ResponseEntity<FuncionarioResponseDTO> obtenerPorDocumento(@PathVariable String documento) {
        return ResponseEntity.ok(funcionarioService.obtenerPorDocumento(documento));
    }
}