package com.fides.sienep.service;

import com.fides.sienep.dto.ComentarioNormalEstudianteRequestDTO;
import com.fides.sienep.dto.ComentarioNormalEstudianteResponseDTO;
import com.fides.sienep.model.ComentarioNormalEstudiante;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.repository.ComentarioNormalEstudianteRepository;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import lombok.*;
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
public class ComentarioNormalEstudianteService {

    private static final Logger log =
            LoggerFactory.getLogger(ComentarioNormalEstudianteService.class);

    private final ComentarioNormalEstudianteRepository comentarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final FuncionarioRepository funcionarioRepository;

    @Transactional(readOnly = true)
    public List<ComentarioNormalEstudianteResponseDTO> listarPorEstudiante(Integer idEstudiante) {
        List<ComentarioNormalEstudiante> comentarios = comentarioRepository
                .findByEstudianteIdUsuarioAndEstado(idEstudiante, EstadoLogico.ACTIVO);
        List<ComentarioNormalEstudianteResponseDTO> resultado = new ArrayList<>();
        for (ComentarioNormalEstudiante c : comentarios) {
            resultado.add(toDTO(c));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public ComentarioNormalEstudianteResponseDTO obtenerPorId(Integer id) {
        return toDTO(buscarOFallar(id));
    }

    @Transactional
    public ComentarioNormalEstudianteResponseDTO crear(
            ComentarioNormalEstudianteRequestDTO dto) {

        Estudiante estudiante = estudianteRepository.findById(dto.getIdEstudiante())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Estudiante no encontrado: " + dto.getIdEstudiante()));

        Funcionario autor = funcionarioRepository.findById(dto.getIdAutor())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Funcionario no encontrado: " + dto.getIdAutor()));

        ComentarioNormalEstudiante comentario =
                new ComentarioNormalEstudiante(dto.getContenido(), autor, estudiante);

        comentarioRepository.save(comentario);
        log.info("Comentario normal creado para estudiante {}", dto.getIdEstudiante());
        return toDTO(comentario);
    }

    @Transactional
    public ComentarioNormalEstudianteResponseDTO editar(
            Integer id, ComentarioNormalEstudianteRequestDTO dto) {

        ComentarioNormalEstudiante comentario = buscarOFallar(id);
        comentario.setContenido(dto.getContenido());
        log.info("Comentario {} editado", id);
        return toDTO(comentarioRepository.save(comentario));
    }

    @Transactional
    public void eliminar(Integer id) {
        ComentarioNormalEstudiante comentario = buscarOFallar(id);
        comentario.setEstado(EstadoLogico.ELIMINADO);
        log.info("Comentario {} dado de baja", id);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private ComentarioNormalEstudiante buscarOFallar(Integer id) {
        return comentarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Comentario no encontrado: " + id));
    }

    private ComentarioNormalEstudianteResponseDTO toDTO(ComentarioNormalEstudiante c) {
        return ComentarioNormalEstudianteResponseDTO.builder()
                .id(c.getId())
                .contenido(c.getContenido())
                .estado(c.getEstado())
                .idEstudiante(c.getEstudiante().getIdUsuario())
                .idAutor(c.getAutor().getIdUsuario())
                .nombreAutor(c.getAutor().getNombre() + " " + c.getAutor().getApellido())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
