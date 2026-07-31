package com.fides.sienep.service;

import com.fides.sienep.dto.ComentarioNormalEstudianteRequestDTO;
import com.fides.sienep.dto.ComentarioNormalEstudianteResponseDTO;
import com.fides.sienep.dto.ComentarioNormalEstudianteUpdateDTO;
import com.fides.sienep.mapper.ComentarioNormalEstudianteMapper;
import com.fides.sienep.model.ComentarioNormalEstudiante;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.repository.ComentarioNormalEstudianteRepository;
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
public class ComentarioNormalEstudianteService {

    private static final Logger log = LoggerFactory.getLogger(ComentarioNormalEstudianteService.class);

    private final ComentarioNormalEstudianteRepository comentarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ComentarioNormalEstudianteMapper comentarioMapper;

    @PreAuthorize("hasAuthority('VER_COMENTARIO_NORMAL_ESTUDIANTE')")
    @Transactional(readOnly = true)
    public List<ComentarioNormalEstudianteResponseDTO> listarPorEstudiante(Integer idEstudiante) {
        Integer idEfectivo = esEstudiante() ? getIdUsuarioAutenticado() : idEstudiante;

        if (idEfectivo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el idEstudiante");
        }

        List<ComentarioNormalEstudiante> comentarios = comentarioRepository
                .findByEstudianteIdUsuarioAndEstado(idEfectivo, EstadoLogico.ACTIVO);
        List<ComentarioNormalEstudianteResponseDTO> resultado = new ArrayList<>();
        for (ComentarioNormalEstudiante c : comentarios) {
            resultado.add(comentarioMapper.toDto(c));
        }
        return resultado;
    }

    @PreAuthorize("hasAuthority('VER_COMENTARIO_NORMAL_ESTUDIANTE')")
    @Transactional(readOnly = true)
    public ComentarioNormalEstudianteResponseDTO obtenerPorId(Integer id) {
        ComentarioNormalEstudiante comentario = buscarOFallar(id);

        if (esEstudiante() && comentario.getEstudiante().getIdUsuario() != getIdUsuarioAutenticado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo podés ver tus propios comentarios");
        }

        return comentarioMapper.toDto(comentario);
    }

    @PreAuthorize("hasAuthority('CREAR_COMENTARIO_NORMAL_ESTUDIANTE')")
    @Transactional
    public ComentarioNormalEstudianteResponseDTO crear(ComentarioNormalEstudianteRequestDTO dto) {

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

        ComentarioNormalEstudiante comentario =
                new ComentarioNormalEstudiante(dto.getContenido(), autor, estudiante);

        comentarioRepository.save(comentario);
        log.info("[AUDIT] Comentario normal creado para estudiante {}", dto.getIdEstudiante());
        return comentarioMapper.toDto(comentario);
    }

    @PreAuthorize("hasAuthority('EDITAR_COMENTARIO_NORMAL_ESTUDIANTE')")
    @Transactional
    public ComentarioNormalEstudianteResponseDTO editar(Integer id, ComentarioNormalEstudianteUpdateDTO dto) {
        ComentarioNormalEstudiante comentario = buscarOFallar(id);
        comentario.setContenido(dto.getContenido());
        log.info("[AUDIT] Comentario {} editado", id);
        return comentarioMapper.toDto(comentarioRepository.save(comentario));
    }

    @PreAuthorize("hasAuthority('ELIMINAR_COMENTARIO_NORMAL_ESTUDIANTE')")
    @Transactional
    public void eliminar(Integer id) {
        ComentarioNormalEstudiante comentario = buscarOFallar(id);
        comentario.setEstado(EstadoLogico.ELIMINADO);
        comentarioRepository.save(comentario);
        log.info("[AUDIT] Comentario {} dado de baja lógica", id);
    }

    private ComentarioNormalEstudiante buscarOFallar(Integer id) {
        return comentarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario no encontrado: " + id));
    }

    private boolean esEstudiante() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        for (var authority : auth.getAuthorities()) {
            if (authority.getAuthority().equals("ROLE_ESTUDIANTE")) return true;
        }
        return false;
    }

    private Integer getIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(auth.getName());
    }
}