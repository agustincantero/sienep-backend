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

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComentarioConfidencialInstanciaService {

    private final ComentarioConfidencialInstanciaRepository comentarioRepository;
    private final InstanciaRepository instanciaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ComentarioConfidencialInstanciaMapper comentarioMapper;

    // Mismo margen que ComentarioNormalInstanciaService: al crear, created_at y updated_at pueden diferir en
    // una fracción de segundo; recién por encima de este margen se considera que el comentario fue editado.
    private static final Duration MARGEN_EDITADO = Duration.ofSeconds(1);

    private Integer getIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(auth.getName());
    }

    @Transactional
    @PreAuthorize("hasAuthority('CREAR_COMENTARIO_CONFIDENCIAL_INSTANCIA')")
    public ComentarioConfidencialInstanciaResponseDTO crear(int codInstancia,
                                                            ComentarioConfidencialInstanciaRequestDTO dto) {
        Instancia instancia = buscarInstanciaOFallar(codInstancia);

        if (instancia.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "La instancia con id " + codInstancia + " no está activa");
        }

        Funcionario autor = funcionarioRepository.findById(getIdUsuarioAutenticado())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró el funcionario autenticado"));

        if (autor.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "El funcionario autenticado no está activo");
        }

        ComentarioConfidencialInstancia comentario = new ComentarioConfidencialInstancia();
        comentario.setInstancia(instancia);
        comentario.setContenido(dto.getContenido());
        comentario.setAutor(autor);

        return aDto(comentarioRepository.save(comentario));
    }

    // Incluye los ELIMINADO, sin su contenido: quedan como marca en el seguimiento, igual que en los
    // comentarios normales (ver ComentarioNormalInstanciaService.listarPorInstancia).
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_COMENTARIO_CONFIDENCIAL_INSTANCIA')")
    public Page<ComentarioConfidencialInstanciaResponseDTO> listarPorInstancia(int codInstancia, Pageable pageable) {
        buscarInstanciaOFallar(codInstancia);
        Page<ComentarioConfidencialInstancia> pagina = comentarioRepository
                .findByInstancia_CodInstanciaAndEstadoIn(codInstancia,
                        List.of(EstadoLogico.ACTIVO, EstadoLogico.ELIMINADO), pageable);
        List<ComentarioConfidencialInstanciaResponseDTO> dtos = pagina.getContent().stream().map(this::aDto).toList();
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }

    // El path es anidado (/instancias/{codInstancia}/comentarios-confidenciales/{id}):
    // se valida que el comentario realmente pertenezca al codInstancia del path
    // (404 si no, mismo criterio que "no encontrado" para no distinguir "no
    // existe" de "existe pero es de otra instancia") — de lo contrario un
    // comentario de la instancia 1 se podría tocar entrando por la URL de la 2.
    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_COMENTARIO_CONFIDENCIAL_INSTANCIA')")
    public ComentarioConfidencialInstanciaResponseDTO editar(int codInstancia, int idComConfidencial,
                                                                 ComentarioConfidencialInstanciaRequestDTO dto) {
        ComentarioConfidencialInstancia comentario = buscarActivoOFallar(idComConfidencial);
        validarPerteneceAInstancia(comentario, codInstancia);
        comentario.setContenido(dto.getContenido());
        // saveAndFlush: el UPDATE sale acá, así updatedAt ya viene con la fecha de esta edición.
        return aDto(comentarioRepository.saveAndFlush(comentario));
    }

    // ELIMINADO (no INACTIVO): unificado con el estado que ya usan los
    // comentarios de Estudiante, ver ComentarioNormalInstanciaService.eliminar.
    @Transactional
    @PreAuthorize("hasAuthority('ELIMINAR_COMENTARIO_CONFIDENCIAL_INSTANCIA')")
    public void eliminar(int codInstancia, int idComConfidencial) {
        ComentarioConfidencialInstancia comentario = buscarActivoOFallar(idComConfidencial);
        validarPerteneceAInstancia(comentario, codInstancia);
        comentario.setEstado(EstadoLogico.ELIMINADO);
        comentarioRepository.save(comentario);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ComentarioConfidencialInstanciaResponseDTO aDto(ComentarioConfidencialInstancia comentario) {
        ComentarioConfidencialInstanciaResponseDTO dto = comentarioMapper.toDto(comentario);
        if (comentario.getEstado() == EstadoLogico.ELIMINADO) {
            dto.setContenido(null);
            return dto;
        }
        dto.setEditado(comentario.getCreatedAt() != null && comentario.getUpdatedAt() != null
                && comentario.getUpdatedAt().isAfter(comentario.getCreatedAt().plus(MARGEN_EDITADO)));
        return dto;
    }

    private void validarPerteneceAInstancia(ComentarioConfidencialInstancia comentario, int codInstancia) {
        if (comentario.getInstancia().getCodInstancia() != codInstancia) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No se encontró el comentario " + comentario.getIdComConfidencial()
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
        return comentarioRepository.findByIdComConfidencialAndEstado(idComConfidencial, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un comentario confidencial activo con el id " + idComConfidencial));
    }
}