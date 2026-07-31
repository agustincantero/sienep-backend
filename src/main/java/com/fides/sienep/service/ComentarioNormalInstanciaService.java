package com.fides.sienep.service;

import com.fides.sienep.dto.ComentarioNormalInstanciaRequestDTO;
import com.fides.sienep.dto.ComentarioNormalInstanciaResponseDTO;
import com.fides.sienep.mapper.ComentarioNormalInstanciaMapper;
import com.fides.sienep.model.ComentarioNormalInstancia;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Instancia;
import com.fides.sienep.repository.ComentarioNormalInstanciaRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.InstanciaRepository;
import lombok.RequiredArgsConstructor;
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
public class ComentarioNormalInstanciaService {

    private final ComentarioNormalInstanciaRepository comentarioRepository;
    private final InstanciaRepository instanciaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ComentarioNormalInstanciaMapper comentarioMapper;

    private Integer getIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(auth.getName());
    }

    @Transactional
    @PreAuthorize("hasAuthority('CREAR_COMENTARIO_NORMAL_INSTANCIA')")
    public ComentarioNormalInstanciaResponseDTO crear(int codInstancia,
                                                      ComentarioNormalInstanciaRequestDTO dto) {
        Instancia instancia = buscarInstanciaOFallar(codInstancia);

        Funcionario autor = funcionarioRepository.findById(getIdUsuarioAutenticado())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró el funcionario autenticado"));

        ComentarioNormalInstancia comentario = new ComentarioNormalInstancia();
        comentario.setContenido(dto.getContenido());
        comentario.setInstancia(instancia);
        comentario.setAutor(autor);

        return comentarioMapper.toDto(comentarioRepository.save(comentario));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_COMENTARIO_NORMAL_INSTANCIA')")
    public List<ComentarioNormalInstanciaResponseDTO> listarPorInstancia(int codInstancia) {
        buscarInstanciaOFallar(codInstancia);
        return comentarioMapper.toDtoList(
                comentarioRepository.findByInstancia_CodInstanciaAndEstado(codInstancia, EstadoLogico.ACTIVO));
    }

    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_COMENTARIO_NORMAL_INSTANCIA')")
    public ComentarioNormalInstanciaResponseDTO actualizar(int idComentario,
                                                           ComentarioNormalInstanciaRequestDTO dto) {
        ComentarioNormalInstancia comentario = buscarActivoOFallar(idComentario);
        comentario.setContenido(dto.getContenido());
        return comentarioMapper.toDto(comentarioRepository.save(comentario));
    }

    // M13 — ELIMINADO (no INACTIVO): a diferencia de los catálogos (Carrera, Grupo,
    // etc.), ningún comentario tiene reactivar(); el permiso que protege este método
    // ya se llama ELIMINAR_COMENTARIO_NORMAL_INSTANCIA, no BAJA_. Unificado con el
    // estado que ya usan los comentarios de Estudiante (ComentarioNormalEstudiante/
    // ComentarioConfidencialEstudiante.eliminar()).
    @Transactional
    @PreAuthorize("hasAuthority('ELIMINAR_COMENTARIO_NORMAL_INSTANCIA')")
    public void darDeBaja(int idComentario) {
        ComentarioNormalInstancia comentario = buscarActivoOFallar(idComentario);
        comentario.setEstado(EstadoLogico.ELIMINADO);
        comentarioRepository.save(comentario);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Instancia buscarInstanciaOFallar(int codInstancia) {
        return instanciaRepository.findById(codInstancia)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una instancia con el id " + codInstancia));
    }

    private ComentarioNormalInstancia buscarActivoOFallar(int idComentario) {
        return comentarioRepository.findByIdComentarioAndEstado(idComentario, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un comentario activo con el id " + idComentario));
    }
}