package com.fides.sienep.service;

import com.fides.sienep.dto.ComentarioConfidencialEstudianteRequestDTO;
import com.fides.sienep.dto.ComentarioConfidencialEstudianteResponseDTO;
import com.fides.sienep.dto.ComentarioConfidencialEstudianteUpdateDTO;
import com.fides.sienep.mapper.ComentarioConfidencialEstudianteMapper;
import com.fides.sienep.model.ComentarioConfidencialEstudiante;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.repository.ComentarioConfidencialEstudianteRepository;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComentarioConfidencialEstudianteService {

    private static final Logger log = LoggerFactory.getLogger(ComentarioConfidencialEstudianteService.class);

    private final ComentarioConfidencialEstudianteRepository comentarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ComentarioConfidencialEstudianteMapper comentarioMapper;

    @PreAuthorize("hasAuthority('VER_COMENTARIO_CONFIDENCIAL_ESTUDIANTE')")
    @Transactional(readOnly = true)
    public List<ComentarioConfidencialEstudianteResponseDTO> listarPorEstudiante(Integer idEstudiante) {
        List<ComentarioConfidencialEstudiante> comentarios = comentarioRepository
                .findByEstudianteIdUsuarioAndEstado(idEstudiante, EstadoLogico.ACTIVO);
        List<ComentarioConfidencialEstudianteResponseDTO> resultado = new ArrayList<>();
        for (ComentarioConfidencialEstudiante c : comentarios) {
            resultado.add(comentarioMapper.toDto(c));
        }
        return resultado;
    }

    @PreAuthorize("hasAuthority('VER_COMENTARIO_CONFIDENCIAL_ESTUDIANTE')")
    @Transactional(readOnly = true)
    public ComentarioConfidencialEstudianteResponseDTO obtenerPorId(Integer id) {
        return comentarioMapper.toDto(buscarOFallar(id));
    }

    @PreAuthorize("hasAuthority('CREAR_COMENTARIO_CONFIDENCIAL_ESTUDIANTE')")
    @Transactional
    public ComentarioConfidencialEstudianteResponseDTO crear(ComentarioConfidencialEstudianteRequestDTO dto) {

        Estudiante estudiante = estudianteRepository.findById(dto.getIdEstudiante())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado: " + dto.getIdEstudiante()));

        if (estudiante.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "El estudiante con id " + dto.getIdEstudiante() + " no está activo");
        }

        Integer idAutor = getIdUsuarioAutenticado();
        Funcionario autor = funcionarioRepository.findById(idAutor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionario no encontrado: " + idAutor));

        if (autor.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "El funcionario autenticado no está activo");
        }

        ComentarioConfidencialEstudiante comentario =
                new ComentarioConfidencialEstudiante(dto.getContenido(), autor, estudiante);

        comentarioRepository.save(comentario);
        log.info("[AUDIT] Observación confidencial creada para estudiante {}", dto.getIdEstudiante());
        return comentarioMapper.toDto(comentario);
    }

    @PreAuthorize("hasAuthority('EDITAR_COMENTARIO_CONFIDENCIAL_ESTUDIANTE')")
    @Transactional
    public ComentarioConfidencialEstudianteResponseDTO editar(Integer id, ComentarioConfidencialEstudianteUpdateDTO dto) {
        ComentarioConfidencialEstudiante comentario = buscarOFallar(id);
        comentario.setContenido(dto.getContenido());
        log.info("[AUDIT] Observación confidencial {} editada", id);
        return comentarioMapper.toDto(comentarioRepository.save(comentario));
    }

    @PreAuthorize("hasAuthority('ELIMINAR_COMENTARIO_CONFIDENCIAL_ESTUDIANTE')")
    @Transactional
    public void eliminar(Integer id) {
        ComentarioConfidencialEstudiante comentario = buscarOFallar(id);
        comentario.setEstado(EstadoLogico.ELIMINADO);
        comentarioRepository.save(comentario);
        log.info("[AUDIT] Observación confidencial {} dada de baja lógica", id);
    }

    private ComentarioConfidencialEstudiante buscarOFallar(Integer id) {
        return comentarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Observación confidencial no encontrada: " + id));
    }

    private Integer getIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(auth.getName());
    }
}