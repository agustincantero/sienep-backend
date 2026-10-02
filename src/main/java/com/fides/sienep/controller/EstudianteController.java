package com.fides.sienep.controller;

import com.fides.sienep.dto.*;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.service.EstudianteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/estudiantes")
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
     *   ?texto=         busca por nombre, apellido o documento (parcial)     requiere BUSCAR_ESTUDIANTE
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
            @ParameterObject @PageableDefault(size = 20, sort = "apellido") Pageable pageable) {
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
    @Operation(summary = "Editar datos de un estudiante",
            description = "Requiere EDITAR_ESTUDIANTE.")
    @ApiResponse(responseCode = "200", description = "Estudiante editado")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    public ResponseEntity<EstudianteResponseDTO> editar(
            @PathVariable Integer id,
            @Valid @RequestBody EstudianteUpdateDTO dto) {
        return ResponseEntity.ok(estudianteService.editar(id, dto));
    }

    @PutMapping("/{id}/mis-datos")
    @Operation(summary = "El estudiante edita sus propios datos de contacto",
            description = "Solo para el estudiante autenticado (ROLE_ESTUDIANTE) sobre su propia cuenta. " +
                    "Permite editar dirección y teléfonos únicamente.")
    @ApiResponse(responseCode = "200", description = "Datos editados")
    @ApiResponse(responseCode = "403", description = "Intento de editar datos de otra cuenta")
    public ResponseEntity<MiPerfilEstudianteDTO> editarMisDatos(
            @PathVariable Integer id,
            @Valid @RequestBody MisDatosRequestDTO dto) {
        return ResponseEntity.ok(estudianteService.editarMisDatos(id, dto));
    }

    @Operation(summary = "Ver mi perfil",
            description = "Permite al estudiante autenticado ver su propio perfil, incluida la dirección " +
                    "(para poder editarla con PUT /{id}/mis-datos). Sin el bloque confidencial ni el " +
                    "motivo de derivación.")
    @ApiResponse(responseCode = "200", description = "Perfil obtenido correctamente")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    @GetMapping("/mi-perfil")
    public ResponseEntity<MiPerfilEstudianteDTO> verMiPerfil() {
        return ResponseEntity.ok(estudianteService.verMiPerfil());
    }

    // =========================================================
    // CONTRASEÑA
    // =========================================================

    @PatchMapping("/{id}/contrasenia")
    @Operation(summary = "Cambiar contraseña",
            description = "Exclusivo del propio estudiante autenticado sobre su propia cuenta — requiere " +
                    "la contraseña actual. Si estaba PENDIENTE_DE_ACTIVACION, pasa a ACTIVO. " +
                    "Para que un funcionario le restablezca la contraseña a un estudiante, usar " +
                    "POST /{id}/contrasenia en su lugar.")
    @ApiResponse(responseCode = "204", description = "Contraseña actualizada")
    @ApiResponse(responseCode = "401", description = "La contraseña actual no es correcta")
    @ApiResponse(responseCode = "403", description = "Intento de cambiar contraseña de otro usuario")
    public ResponseEntity<Void> cambiarContrasenia(
            @PathVariable Integer id,
            @Valid @RequestBody CambiarContraseniaDTO dto) {
        estudianteService.cambiarContrasenia(id, dto);
        return ResponseEntity.noContent().build();
    }

    // Mismo sub-recurso que cambiarContrasenia (/{id}/contrasenia): acá el cliente no manda
    // ningún body porque no es él quien define el valor nuevo, lo genera el servidor y lo
    // manda por email. POST (no PATCH) porque es una acción con efecto secundario, no una
    // modificación de estado descrita por el cliente — mismo criterio que se usó en RF27
    // para no meter un verbo en la URL (ver crearInstanciaDesdeRecordatorio).
    @PostMapping("/{id}/contrasenia")
    @Operation(summary = "Restablecer contraseña de un estudiante",
            description = "Requiere EDITAR_ESTUDIANTE. Genera una nueva contraseña y la envía por email.")
    @ApiResponse(responseCode = "204", description = "Contraseña restablecida y email enviado")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    public ResponseEntity<Void> restablecerContrasenia(@PathVariable Integer id) {
        estudianteService.restablecerContrasenia(id);
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
    @GetMapping("/{id}/procesos-abiertos")
    @Operation(summary = "Procesos abiertos de un estudiante (antes de darlo de baja)",
            description = "Requiere DESACTIVAR_ESTUDIANTE. Cuenta las instancias agendadas a futuro y los recordatorios " +
                    "pendientes, para avisarlos antes de la baja (RF06).")
    @ApiResponse(responseCode = "200", description = "Conteo de procesos abiertos")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    public ResponseEntity<ProcesosAbiertosDTO> procesosAbiertos(@PathVariable Integer id) {
        return ResponseEntity.ok(estudianteService.procesosAbiertos(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar un estudiante",
            description = "Requiere DESACTIVAR_ESTUDIANTE. No elimina datos — estado pasa a INACTIVO. Si el estudiante " +
                    "tiene procesos abiertos (ver GET /{id}/procesos-abiertos), hay que mandar confirmar=true.")
    @ApiResponse(responseCode = "204", description = "Estudiante desactivado")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    @ApiResponse(responseCode = "409", description = "Tiene procesos abiertos y no se confirmó la baja")
    public ResponseEntity<Void> desactivar(
            @PathVariable Integer id,
            @Parameter(description = "true para confirmar la baja aunque tenga procesos abiertos")
            @RequestParam(defaultValue = "false") boolean confirmar) {
        estudianteService.desactivar(id, confirmar);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivación de un estudiante dado de baja.
     * PATCH sobre el sub-recurso /estado — modifica parcialmente el estado del recurso.
     * El DTO debe traer estado = ACTIVO. Si trae otro valor, se rechaza con 400.
     */
    @PatchMapping("/{id}/reactivar")
    @Operation(summary = "Reactivar un estudiante desactivado",
            description = "Requiere REACTIVAR_ESTUDIANTE. Sin body — la URL define la acción.")
    @ApiResponse(responseCode = "204", description = "Estudiante reactivado")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    public ResponseEntity<Void> reactivar(@PathVariable Integer id) {
        estudianteService.reactivar(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // FOTO DE PERFIL
    // =========================================================

    @PutMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir o reemplazar la foto de perfil de un estudiante",
            description = "Requiere EDITAR_ESTUDIANTE, o ser el propio estudiante autenticado.")
    @ApiResponse(responseCode = "200", description = "Foto actualizada")
    @ApiResponse(responseCode = "400", description = "Extensión no permitida, archivo vacío o demasiado grande")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    public ResponseEntity<EstudianteResponseDTO> subirFoto(
            @PathVariable Integer id,
            @RequestParam("foto") MultipartFile foto) {
        return ResponseEntity.ok(estudianteService.subirFoto(id, foto));
    }

    @GetMapping("/{id}/foto")
    @Operation(summary = "Ver la foto de perfil de un estudiante",
            description = "Requiere VER_ESTUDIANTE, o ser el propio estudiante autenticado.")
    @ApiResponse(responseCode = "200", description = "Foto obtenida")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado o sin foto cargada")
    public ResponseEntity<Resource> verFoto(@PathVariable Integer id) {
        ArchivoDescargaDTO archivo = estudianteService.obtenerFoto(id);
        return ResponseEntity.ok()
                .contentType(archivo.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + archivo.nombreDescarga() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(archivo.recurso());
    }
}