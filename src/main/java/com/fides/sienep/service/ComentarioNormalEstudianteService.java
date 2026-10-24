package com.fides.sienep.service;

import com.fides.sienep.dto.ComentarioNormalEstudianteRequestDTO;
import com.fides.sienep.dto.ComentarioNormalEstudianteResponseDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    public Page<ComentarioNormalEstudianteResponseDTO> listarPorEstudiante(Integer idEstudiante, Pageable pageable) {
        Integer idEfectivo = esEstudiante() ? getIdUsuarioAutenticado() : idEstudiante;

        if (idEfectivo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el idEstudiante");
        }

        Page<ComentarioNormalEstudiante> pagina = comentarioRepository
                .findByEstudianteIdUsuarioAndEstado(idEfectivo, EstadoLogico.ACTIVO, pageable);
        List<ComentarioNormalEstudianteResponseDTO> dtos = comentarioMapper.toDtoList(pagina.getContent());
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
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
        log.info("Comentario normal creado para estudiante {}", dto.getIdEstudiante());
        return comentarioMapper.toDto(comentario);
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