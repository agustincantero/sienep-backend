package com.fides.sienep.service;

import com.fides.sienep.dto.InformeAdjuntoArchivoDTO;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InformeAdjuntoService {

    private static final Logger log = LoggerFactory.getLogger(InformeAdjuntoService.class);

    // RF09 — extensiones y tamaño máximo permitidos para el archivo adjunto.
    private static final List<String> EXTENSIONES_PERMITIDAS = List.of("pdf", "jpg", "jpeg", "png");
    private static final long MAX_TAMANIO_BYTES = 5L * 1024 * 1024;

    private final InformeAdjuntoRepository informeRepository;
    private final EstudianteRepository estudianteRepository;
    private final InformeAdjuntoMapper informeMapper;

    // Campo separado (no final): Lombok no copia @Value a los parámetros del
    // constructor generado por @RequiredArgsConstructor sin un lombok.config,
    // por lo que esta property se inyecta directamente sobre el campo.
    @Value("${app.storage.informes-dir}")
    private String storageDir;

    @PreAuthorize("hasAuthority('VER_ADJUNTOS')")
    @Transactional(readOnly = true)
    public List<InformeAdjuntoResponseDTO> listarPorEstudiante(Integer idUsuario) {
        List<InformeAdjunto> informes = informeRepository
                .findByEstudianteIdUsuarioAndEstado(idUsuario, EstadoLogico.ACTIVO);
        List<InformeAdjuntoResponseDTO> resultado = new ArrayList<>();
        for (InformeAdjunto i : informes) {
            resultado.add(informeMapper.toDto(i));
        }
        return resultado;
    }

    @PreAuthorize("hasAuthority('VER_ADJUNTOS')")
    @Transactional(readOnly = true)
    public InformeAdjuntoResponseDTO obtenerPorId(Integer id) {
        return informeMapper.toDto(buscarOFallar(id));
    }

    @PreAuthorize("hasAuthority('ADJUNTAR_INFORME')")
    @Transactional
    public InformeAdjuntoResponseDTO adjuntar(InformeAdjuntoRequestDTO dto) {

        Estudiante estudiante = estudianteRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Estudiante no encontrado: " + dto.getIdUsuario()));

        if (informeRepository.existsByEstudianteIdUsuarioAndNombre(dto.getIdUsuario(), dto.getNombre())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Ya existe un informe con ese nombre para este estudiante");
        }

        MultipartFile archivo = dto.getArchivo();
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo es obligatorio");
        }

        if (archivo.getSize() > MAX_TAMANIO_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo supera el tamaño máximo permitido de 5MB");
        }

        String extension = extraerExtension(archivo.getOriginalFilename());
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La extensión del archivo no está permitida. Use PDF, JPG, JPEG o PNG");
        }

        String subdirectorio = String.valueOf(dto.getIdUsuario());
        String nombreFisico = UUID.randomUUID() + "." + extension;
        guardarEnDisco(archivo, subdirectorio, nombreFisico);

        InformeAdjunto informe = InformeAdjunto.builder()
                .estudiante(estudiante)
                .urlArchivo(nombreFisico)
                .ruta(subdirectorio)
                .nombre(dto.getNombre())
                .build();

        informeRepository.save(informe);
        log.info("[AUDIT] Informe adjuntado al estudiante {}", dto.getIdUsuario());
        return informeMapper.toDto(informe);
    }

    @PreAuthorize("hasAuthority('VER_ADJUNTOS')")
    @Transactional(readOnly = true)
    public InformeAdjuntoArchivoDTO descargar(Integer id) {
        InformeAdjunto informe = buscarOFallar(id);

        Path ruta = Path.of(storageDir, informe.getRuta(), informe.getUrlArchivo());
        if (!Files.exists(ruta)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El archivo físico del informe " + id + " no se encuentra disponible");
        }

        String extension = extraerExtension(informe.getUrlArchivo());
        Resource recurso = new FileSystemResource(ruta);
        MediaType contentType = mediaTypeDeExtension(extension);
        String nombreDescarga = informe.getNombre() + "." + extension;

        return new InformeAdjuntoArchivoDTO(recurso, contentType, nombreDescarga);
    }

    @PreAuthorize("hasAuthority('ELIMINAR_INFORME')")
    @Transactional
    public void eliminar(Integer id) {
        InformeAdjunto informe = buscarOFallar(id);
        informe.setEstado(EstadoLogico.ELIMINADO);
        informeRepository.save(informe);
        log.info("[AUDIT] Informe {} dado de baja lógica", id);
    }

    private InformeAdjunto buscarOFallar(Integer id) {
        return informeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Informe no encontrado: " + id));
    }

    private String extraerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La extensión del archivo no está permitida. Use PDF, JPG, JPEG o PNG");
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private MediaType mediaTypeDeExtension(String extension) {
        return switch (extension) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private void guardarEnDisco(MultipartFile archivo, String subdirectorio, String nombreFisico) {
        try {
            Path directorio = Path.of(storageDir, subdirectorio);
            Files.createDirectories(directorio);
            archivo.transferTo(directorio.resolve(nombreFisico));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar el archivo en el servidor", e);
        }
    }
}
