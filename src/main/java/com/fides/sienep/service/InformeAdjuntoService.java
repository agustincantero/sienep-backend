package com.fides.sienep.service;

import com.fides.sienep.dto.ArchivoDescargaDTO;
import com.fides.sienep.dto.InformeAdjuntoRequestDTO;
import com.fides.sienep.dto.InformeAdjuntoResponseDTO;
import com.fides.sienep.mapper.InformeAdjuntoMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.InformeAdjunto;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.InformeAdjuntoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InformeAdjuntoService {

    private static final Logger log = LoggerFactory.getLogger(InformeAdjuntoService.class);

    // RF09 — extensiones y tamaño máximo permitidos para el archivo adjunto.
    private static final List<String> EXTENSIONES_PERMITIDAS = List.of("pdf", "jpg", "jpeg", "png");
    private static final long MAX_TAMANIO_BYTES = 5L * 1024 * 1024;
    private static final String SUBCARPETA_BASE = "informes";

    private final InformeAdjuntoRepository informeRepository;
    private final EstudianteRepository estudianteRepository;
    private final InformeAdjuntoMapper informeMapper;
    private final FileStorageService fileStorageService;

    // Incluye los ELIMINADO, sin urlDescarga (ver InformeAdjuntoMapper): quedan como marca, para que quien ve
    // la ficha sepa que hubo un informe (y pueda pedir la auditoría) en vez de que desaparezca sin rastro.
    // Mismo criterio que los comentarios de instancia. El listado del propio estudiante (listarMios) no cambia.
    @PreAuthorize("hasAuthority('VER_INFORME')")
    @Transactional(readOnly = true)
    public Page<InformeAdjuntoResponseDTO> listarPorEstudiante(Integer idUsuario, Pageable pageable) {
        Page<InformeAdjunto> pagina = informeRepository
                .findByEstudianteIdUsuarioAndEstadoIn(idUsuario,
                        List.of(EstadoLogico.ACTIVO, EstadoLogico.ELIMINADO), pageable);
        List<InformeAdjuntoResponseDTO> dtos = informeMapper.toDtoList(pagina.getContent());
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }

    @PreAuthorize("hasAuthority('ADJUNTAR_INFORME')")
    @Transactional
    public InformeAdjuntoResponseDTO adjuntar(InformeAdjuntoRequestDTO dto) {

        Estudiante estudiante = estudianteRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Estudiante no encontrado: " + dto.getIdUsuario()));

        // PENDIENTE_DE_ACTIVACION también se admite: es el estado con el que se crea todo
        // estudiante hasta su primer login (y al que vuelve tras un reset de contraseña), y
        // no debe impedir cargarle informes. Mismo criterio que restablecerContrasenia.
        if (estudiante.getEstado() != EstadoLogico.ACTIVO
                && estudiante.getEstado() != EstadoLogico.PENDIENTE_DE_ACTIVACION) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "No se puede adjuntar un informe a un estudiante inactivo.");
        }

        // Los informes eliminados no reservan el nombre (mismo criterio que uk_informe_nombre_no_eliminado).
        if (informeRepository.existsByEstudianteIdUsuarioAndNombreAndEstadoNot(
                dto.getIdUsuario(), dto.getNombre(), EstadoLogico.ELIMINADO)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Ya existe un informe con ese nombre para este estudiante");
        }

        String subdirectorio = SUBCARPETA_BASE + "/" + dto.getIdUsuario();
        String nombreFisico = fileStorageService.guardar(
                dto.getArchivo(), subdirectorio, EXTENSIONES_PERMITIDAS, MAX_TAMANIO_BYTES);

        InformeAdjunto informe = InformeAdjunto.builder()
                .estudiante(estudiante)
                .urlArchivo(nombreFisico)
                .ruta(subdirectorio)
                .nombre(dto.getNombre())
                .build();

        try {
            informeRepository.save(informe);
        } catch (RuntimeException e) {
            fileStorageService.eliminarSiExiste(subdirectorio + "/" + nombreFisico);
            throw e;
        }
        log.info("[AUDIT] Informe adjuntado al estudiante {}", dto.getIdUsuario());
        return informeMapper.toDto(informe);
    }

    @PreAuthorize("hasAuthority('VER_INFORME')")
    @Transactional(readOnly = true)
    public ArchivoDescargaDTO descargar(Integer id) {
        return aArchivoDescarga(buscarActivoOFallar(id));
    }

    private ArchivoDescargaDTO aArchivoDescarga(InformeAdjunto informe) {
        Resource recurso = fileStorageService.cargar(informe.getRuta() + "/" + informe.getUrlArchivo());
        String extension = fileStorageService.extraerExtension(informe.getUrlArchivo());
        MediaType contentType = fileStorageService.mediaTypeDeExtension(extension);
        String nombreDescarga = informe.getNombre() + "." + extension;

        return new ArchivoDescargaDTO(recurso, contentType, nombreDescarga);
    }

    @PreAuthorize("hasAuthority('ELIMINAR_INFORME')")
    @Transactional
    public void eliminar(Integer id) {
        InformeAdjunto informe = buscarActivoOFallar(id);
        informe.setEstado(EstadoLogico.ELIMINADO);
        informeRepository.save(informe);
        log.info("[AUDIT] Informe {} dado de baja lógica", id);
    }

    // =========================================================
    // AUTOSERVICIO DEL ESTUDIANTE ("Mi perfil")
    // =========================================================
    // Endpoints separados de los de funcionarios a propósito: los de arriba exigen VER_INFORME /
    // ADJUNTAR_INFORME / ELIMINAR_INFORME y no tienen ninguna excepción para el estudiante. Acá el estudiante
    // solo puede ver y descargar sus propios informes (no subirlos ni borrarlos), y su id sale siempre del
    // token, nunca de un parámetro.

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public Page<InformeAdjuntoResponseDTO> listarMios(Pageable pageable) {
        return informeRepository
                .findByEstudianteIdUsuarioAndEstado(getIdUsuarioAutenticado(), EstadoLogico.ACTIVO, pageable)
                .map(informeMapper::toDtoDelEstudiante);
    }

    // Si el informe es de otro estudiante responde 404, igual que si no existiera: no revela que existe.
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public ArchivoDescargaDTO descargarMio(Integer id) {
        InformeAdjunto informe = informeRepository
                .findByIdInformeAndEstudianteIdUsuarioAndEstado(id, getIdUsuarioAutenticado(), EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Informe no encontrado: " + id));
        return aArchivoDescarga(informe);
    }

    private int getIdUsuarioAutenticado() {
        return Integer.parseInt(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private InformeAdjunto buscarActivoOFallar(Integer id) {
        return informeRepository.findByIdInformeAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Informe no encontrado: " + id));
    }
}
