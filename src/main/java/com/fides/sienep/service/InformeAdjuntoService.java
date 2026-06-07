package com.fides.sienep.service;

import com.fides.sienep.dto.InformeAdjuntoRequestDTO;
import com.fides.sienep.dto.InformeAdjuntoResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.InformeAdjunto;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.InformeAdjuntoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InformeAdjuntoService {

    private static final Logger log = LoggerFactory.getLogger(InformeAdjuntoService.class);

    private final InformeAdjuntoRepository informeRepository;
    private final EstudianteRepository estudianteRepository;

    @Transactional(readOnly = true)
    public List<InformeAdjuntoResponseDTO> listarPorEstudiante(Integer idUsuario) {
        List<InformeAdjunto> informes = informeRepository
                .findByEstudianteIdUsuarioAndEstado(idUsuario, EstadoLogico.ACTIVO);
        List<InformeAdjuntoResponseDTO> resultado = new ArrayList<>();
        for (InformeAdjunto i : informes) {
            resultado.add(toDTO(i));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public InformeAdjuntoResponseDTO obtenerPorId(Integer id) {
        return toDTO(buscarOFallar(id));
    }

    @Transactional
    public InformeAdjuntoResponseDTO adjuntar(InformeAdjuntoRequestDTO dto) {

        Estudiante estudiante = estudianteRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Estudiante no encontrado: " + dto.getIdUsuario()));

        if (informeRepository.existsByEstudianteIdUsuarioAndNombre(
                dto.getIdUsuario(), dto.getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un informe con ese nombre para este estudiante");
        }

        // Validar extensión permitida
        String url = dto.getUrlArchivo().toLowerCase();
        if (!url.endsWith(".pdf") && !url.endsWith(".jpg")
                && !url.endsWith(".jpeg") && !url.endsWith(".png")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La extensión del archivo no está permitida. Use PDF, JPG, JPEG o PNG");
        }

        InformeAdjunto informe = InformeAdjunto.builder()
                .estudiante(estudiante)
                .urlArchivo(dto.getUrlArchivo())
                .ruta(dto.getRuta())
                .nombre(dto.getNombre())
                .build();

        informeRepository.save(informe);
        log.info("Informe adjuntado al estudiante {}", dto.getIdUsuario());
        return toDTO(informe);
    }

    @Transactional
    public void eliminar(Integer id) {
        InformeAdjunto informe = buscarOFallar(id);
        informe.setEstado(EstadoLogico.ELIMINADO);
        log.info("Informe {} dado de baja", id);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private InformeAdjunto buscarOFallar(Integer id) {
        return informeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Informe no encontrado: " + id));
    }

    private InformeAdjuntoResponseDTO toDTO(InformeAdjunto i) {
        return InformeAdjuntoResponseDTO.builder()
                .idInforme(i.getIdInforme())
                .idUsuario(i.getEstudiante().getIdUsuario())
                .urlArchivo(i.getUrlArchivo())
                .ruta(i.getRuta())
                .nombre(i.getNombre())
                .estado(i.getEstado())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }
}
