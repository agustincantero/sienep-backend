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
import org.springframework.security.core.Authentication;
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

    // El propio estudiante también puede listar los suyos (autoservicio desde "Mi perfil"), sin VER_INFORME.
    @PreAuthorize("hasAuthority('VER_INFORME') or authentication.name == #idUsuario?.toString()")
    @Transactional(readOnly = true)
    public Page<InformeAdjuntoResponseDTO> listarPorEstudiante(Integer idUsuario, Pageable pageable) {
        Page<InformeAdjunto> pagina = informeRepository
                .findByEstudianteIdUsuarioAndEstado(idUsuario, EstadoLogico.ACTIVO, pageable);
        List<InformeAdjuntoResponseDTO> dtos = informeMapper.toDtoList(pagina.getContent());
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }

    // Sin VER_INFORME, un estudiante solo puede ver los suyos: el dueño se conoce recién al cargar el
    // informe, así que se valida adentro (verificarAcceso) y no en el @PreAuthorize.
    @PreAuthorize("hasAuthority('VER_INFORME') or hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public InformeAdjuntoResponseDTO obtenerPorId(Integer id) {
        return informeMapper.toDto(verificarAcceso(buscarActivoOFallar(id)));
    }

    // El propio estudiante puede adjuntar informes a su historia (autoservicio desde "Mi perfil"), sin
    // ADJUNTAR_INFORME. No a otro estudiante: authentication.name es su idUsuario. Borrar sigue exigiendo
    // ELIMINAR_INFORME, para que no se pierda documentación aportada.
    @PreAuthorize("hasAuthority('ADJUNTAR_INFORME') or authentication.name == #dto.idUsuario?.toString()")
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

        if (informeRepository.existsByEstudianteIdUsuarioAndNombre(dto.getIdUsuario(), dto.getNombre())) {
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

    // Mismo criterio que obtenerPorId: sin VER_INFORME, solo los informes propios.
    @PreAuthorize("hasAuthority('VER_INFORME') or hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public ArchivoDescargaDTO descargar(Integer id) {
        InformeAdjunto informe = verificarAcceso(buscarActivoOFallar(id));

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

    // Quien no tiene VER_INFORME (un estudiante) solo accede a informes de su propia historia.
    private InformeAdjunto verificarAcceso(InformeAdjunto informe) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean puedeVerTodos = auth.getAuthorities().stream()
                .anyMatch(a -> "VER_INFORME".equals(a.getAuthority()));
        boolean esPropio = String.valueOf(informe.getEstudiante().getIdUsuario()).equals(auth.getName());
        if (!puedeVerTodos && !esPropio) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes ver tus propios informes.");
        }
        return informe;
    }

    private InformeAdjunto buscarActivoOFallar(Integer id) {
        return informeRepository.findByIdInformeAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Informe no encontrado: " + id));
    }
}
