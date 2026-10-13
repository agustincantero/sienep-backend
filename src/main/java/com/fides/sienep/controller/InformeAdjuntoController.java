package com.fides.sienep.controller;

import com.fides.sienep.dto.ArchivoDescargaDTO;
import com.fides.sienep.dto.InformeAdjuntoRequestDTO;
import com.fides.sienep.dto.InformeAdjuntoResponseDTO;
import com.fides.sienep.service.InformeAdjuntoService;
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

@RestController
@RequestMapping("/informes-adjuntos")
@RequiredArgsConstructor
@Tag(name = "Informes Adjuntos", description = "Gestión de informes adjuntos de estudiantes")
public class InformeAdjuntoController {

    private final InformeAdjuntoService informeService;

    @GetMapping
    @Operation(summary = "Listar informes de un estudiante",
            description = "En orden de carga. Los eliminados se incluyen como marca: estado ELIMINADO y sin urlDescarga.")
    @ApiResponse(responseCode = "200", description = "Página de informes")
    public ResponseEntity<Page<InformeAdjuntoResponseDTO>> listar(
            @RequestParam Integer idUsuario,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(informeService.listarPorEstudiante(idUsuario, pageable));
    }

    @GetMapping("/mios")
    @Operation(summary = "Ver mis informes",
            description = "Solo el estudiante autenticado: sus propios informes activos. Los informes los sube " +
                    "únicamente el personal (POST /informes-adjuntos, requiere ADJUNTAR_INFORME).")
    @ApiResponse(responseCode = "200", description = "Página de informes del estudiante")
    public ResponseEntity<Page<InformeAdjuntoResponseDTO>> listarMios(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(informeService.listarMios(pageable));
    }

    @GetMapping("/mios/{id}/descargar")
    @Operation(summary = "Descargar o ver uno de mis informes",
            description = "Solo el estudiante autenticado y solo si el informe es suyo (si no, 404). Con inline=true " +
                    "el navegador lo muestra en lugar de descargarlo (vista previa).")
    @ApiResponse(responseCode = "200", description = "Archivo")
    @ApiResponse(responseCode = "404", description = "Informe no encontrado o de otro estudiante")
    public ResponseEntity<Resource> descargarMio(
            @PathVariable Integer id,
            @Parameter(description = "true para ver el archivo en el navegador (vista previa) en vez de descargarlo")
            @RequestParam(defaultValue = "false") boolean inline) {
        return respuestaDeDescarga(informeService.descargarMio(id), inline);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener informe por ID")
    @ApiResponse(responseCode = "200", description = "Informe encontrado")
    @ApiResponse(responseCode = "404", description = "Informe no encontrado")
    public ResponseEntity<InformeAdjuntoResponseDTO> obtenerPorId(
            @PathVariable Integer id) {
        return ResponseEntity.ok(informeService.obtenerPorId(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Adjuntar informe a un estudiante")
    @ApiResponse(responseCode = "201", description = "Informe adjuntado")
    @ApiResponse(responseCode = "400", description = "Extensión no permitida, archivo vacío o demasiado grande")
    @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    @ApiResponse(responseCode = "409", description = "Ya existe un informe con ese nombre para este estudiante")
    public ResponseEntity<InformeAdjuntoResponseDTO> adjuntar(
            @Valid @ModelAttribute InformeAdjuntoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(informeService.adjuntar(dto));
    }

    @GetMapping("/{id}/descargar")
    @Operation(summary = "Descargar o ver el archivo de un informe adjunto",
            description = "Por defecto se descarga (Content-Disposition: attachment). Con inline=true el navegador " +
                    "lo muestra en lugar de descargarlo (vista previa).")
    @ApiResponse(responseCode = "200", description = "Archivo")
    @ApiResponse(responseCode = "404", description = "Informe no encontrado")
    public ResponseEntity<Resource> descargar(
            @PathVariable Integer id,
            @Parameter(description = "true para ver el archivo en el navegador (vista previa) en vez de descargarlo")
            @RequestParam(defaultValue = "false") boolean inline) {
        return respuestaDeDescarga(informeService.descargar(id), inline);
    }

    // RF09 — vista previa: con inline el navegador muestra el archivo (visor de PDF o imagen) en vez de
    // descargarlo. Es seguro mostrarlo así: al adjuntar solo se aceptan PDF/JPG/PNG, el Content-Type sale de la
    // extensión (FileStorageService) y nosniff impide que el navegador lo interprete como otra cosa.
    private ResponseEntity<Resource> respuestaDeDescarga(ArchivoDescargaDTO archivo, boolean inline) {
        return ResponseEntity.ok()
                .contentType(archivo.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (inline ? "inline" : "attachment") + "; filename=\"" + archivo.nombreDescarga() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(archivo.recurso());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Baja lógica de informe adjunto")
    @ApiResponse(responseCode = "204", description = "Informe eliminado")
    @ApiResponse(responseCode = "404", description = "Informe no encontrado")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        informeService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}