package com.fides.sienep.service;

import com.fides.sienep.dto.ComentarioConfidencialInstanciaRequestDTO;
import com.fides.sienep.dto.ComentarioConfidencialInstanciaResponseDTO;
import com.fides.sienep.mapper.ComentarioConfidencialInstanciaMapper;
import com.fides.sienep.model.ComentarioConfidencialInstancia;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Instancia;
import com.fides.sienep.repository.ComentarioConfidencialInstanciaRepository;
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
public class ComentarioConfidencialInstanciaService {

    private final ComentarioConfidencialInstanciaRepository comentarioRepository;
    private final InstanciaRepository instanciaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ComentarioConfidencialInstanciaMapper comentarioMapper;

    private Integer getIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(auth.getName());
    }

    @Transactional
    @PreAuthorize("hasAuthority('CREAR_COMENTARIO_CONFIDENCIAL_INSTANCIA')")
    public ComentarioConfidencialInstanciaResponseDTO crear(int codInstancia,
                                                            ComentarioConfidencialInstanciaRequestDTO dto) {
        Instancia instancia = buscarInstanciaOFallar(codInstancia);

        Funcionario autor = funcionarioRepository.findById(getIdUsuarioAutenticado())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró el funcionario autenticado"));

        ComentarioConfidencialInstancia comentario = new ComentarioConfidencialInstancia();
        comentario.setId(new ComentarioConfidencialInstancia.ComentarioConfidencialInstanciaId());
        comentario.setInstancia(instancia);
        comentario.setContenido(dto.getContenido());
        comentario.setAutor(autor);

        return comentarioMapper.toDto(comentarioRepository.save(comentario));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_COMENTARIO_CONFIDENCIAL_INSTANCIA')")
    public List<ComentarioConfidencialInstanciaResponseDTO> listarPorInstancia(int codInstancia) {
        buscarInstanciaOFallar(codInstancia);
        return comentarioMapper.toDtoList(
                comentarioRepository.findByInstancia_CodInstanciaAndEstado(codInstancia, EstadoLogico.ACTIVO));
    }

    // El path es anidado (/instancias/{codInstancia}/comentarios-confidenciales/{id}):
    // se valida que el comentario realmente pertenezca al codInstancia del path
    // (404 si no, mismo criterio que "no encontrado" para no distinguir "no
    // existe" de "existe pero es de otra instancia") — de lo contrario un
    // comentario de la instancia 1 se podría tocar entrando por la URL de la 2.
    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_COMENTARIO_CONFIDENCIAL_INSTANCIA')")
    public ComentarioConfidencialInstanciaResponseDTO actualizar(int codInstancia, int idComConfidencial,
                                                                 ComentarioConfidencialInstanciaRequestDTO dto) {
        ComentarioConfidencialInstancia comentario = buscarActivoOFallar(idComConfidencial);
        validarPerteneceAInstancia(comentario, codInstancia);
        comentario.setContenido(dto.getContenido());
        return comentarioMapper.toDto(comentarioRepository.save(comentario));
    }

    // ELIMINADO (no INACTIVO): unificado con el estado que ya usan los
    // comentarios de Estudiante, ver ComentarioNormalInstanciaService.darDeBaja.
    @Transactional
    @PreAuthorize("hasAuthority('ELIMINAR_COMENTARIO_CONFIDENCIAL_INSTANCIA')")
    public void darDeBaja(int codInstancia, int idComConfidencial) {
        ComentarioConfidencialInstancia comentario = buscarActivoOFallar(idComConfidencial);
        validarPerteneceAInstancia(comentario, codInstancia);
        comentario.setEstado(EstadoLogico.ELIMINADO);
        comentarioRepository.save(comentario);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void validarPerteneceAInstancia(ComentarioConfidencialInstancia comentario, int codInstancia) {
        if (comentario.getInstancia().getCodInstancia() != codInstancia) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No se encontró el comentario " + comentario.getId().getIdComConfidencial()
                            + " en la instancia " + codInstancia);
        }
    }

    private Instancia buscarInstanciaOFallar(int codInstancia) {
        return instanciaRepository.findById(codInstancia)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una instancia con el id " + codInstancia));
    }

    private ComentarioConfidencialInstancia buscarActivoOFallar(int idComConfidencial) {
        return comentarioRepository.findById_IdComConfidencialAndEstado(idComConfidencial, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un comentario confidencial activo con el id " + idComConfidencial));
    }
}