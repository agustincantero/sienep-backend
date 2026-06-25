package com.fides.sienep.service;

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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InformeAdjuntoService {

    private static final Logger log = LoggerFactory.getLogger(InformeAdjuntoService.class);

    // RF09 — tamaño máximo de URL permitido. El sistema gestiona archivos por URL/ruta,
    // no por carga directa (MultipartFile). La validación de tamaño real del archivo
    // (ej: máximo 5MB) corresponde al servicio de almacenamiento externo o al cliente.
    // Si en el futuro se migra a carga directa, agregar:
    // if (file.getSize() > 5 * 1024 * 1024) throw new ResponseStatusException(BAD_REQUEST, ...)
    private static final int MAX_URL_LENGTH = 500;

    private final InformeAdjuntoRepository informeRepository;
    private final EstudianteRepository estudianteRepository;
    private final InformeAdjuntoMapper informeMapper;

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

        // Validación de extensión (RF09)
        String url = dto.getUrlArchivo().toLowerCase();
        if (!url.endsWith(".pdf") && !url.endsWith(".jpg")
                && !url.endsWith(".jpeg") && !url.endsWith(".png")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La extensión del archivo no está permitida. Use PDF, JPG, JPEG o PNG");
        }

        // Validación de longitud de URL (RF09)
        if (dto.getUrlArchivo().length() > MAX_URL_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La URL del archivo supera el tamaño máximo permitido de " + MAX_URL_LENGTH + " caracteres");
        }

        InformeAdjunto informe = InformeAdjunto.builder()
                .estudiante(estudiante)
                .urlArchivo(dto.getUrlArchivo())
                .ruta(dto.getRuta())
                .nombre(dto.getNombre())
                .build();

        informeRepository.save(informe);
        log.info("[AUDIT] Informe adjuntado al estudiante {}", dto.getIdUsuario());
        return informeMapper.toDto(informe);
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
}