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
public class ComentarioNormalInstanciaService {

    private final ComentarioNormalInstanciaRepository comentarioRepository;
    private final InstanciaRepository instanciaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ComentarioNormalInstanciaMapper comentarioMapper;

    // Al crear, Hibernate genera created_at y updated_at por separado y pueden diferir en una fracción de
    // segundo: recién por encima de este margen se considera que el comentario fue editado.
    private static final Duration MARGEN_EDITADO = Duration.ofSeconds(1);

    private Integer getIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(auth.getName());
    }

    @Transactional
    @PreAuthorize("hasAuthority('CREAR_COMENTARIO_NORMAL_INSTANCIA')")
    public ComentarioNormalInstanciaResponseDTO crear(int codInstancia,
                                                      ComentarioNormalInstanciaRequestDTO dto) {
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

        ComentarioNormalInstancia comentario = new ComentarioNormalInstancia();
        comentario.setContenido(dto.getContenido());
        comentario.setInstancia(instancia);
        comentario.setAutor(autor);

        return aDto(comentarioRepository.save(comentario));
    }

    // Incluye los ELIMINADO, sin su contenido: quedan como marca en el seguimiento, para que quien lo lee
    // sepa que ahí hubo un comentario (y pueda pedir la auditoría) en vez de encontrarse con un hueco.
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_COMENTARIO_NORMAL_INSTANCIA')")
    public Page<ComentarioNormalInstanciaResponseDTO> listarPorInstancia(int codInstancia, Pageable pageable) {
        buscarInstanciaOFallar(codInstancia);
        Page<ComentarioNormalInstancia> pagina = comentarioRepository
                .findByInstancia_CodInstanciaAndEstadoIn(codInstancia,
                        List.of(EstadoLogico.ACTIVO, EstadoLogico.ELIMINADO), pageable);
        List<ComentarioNormalInstanciaResponseDTO> dtos = pagina.getContent().stream().map(this::aDto).toList();
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_COMENTARIO_NORMAL_INSTANCIA')")
    public ComentarioNormalInstanciaResponseDTO obtenerPorId(int codInstancia, int idComentario) {
        ComentarioNormalInstancia comentario = buscarActivoOFallar(idComentario);
        validarPerteneceAInstancia(comentario, codInstancia);
        return aDto(comentario);
    }

    // El path es anidado (/instancias/{codInstancia}/comentarios-normales/{id}):
    // se valida que el comentario realmente pertenezca al codInstancia del path
    // (404 si no, mismo criterio que "no encontrado" para no distinguir "no
    // existe" de "existe pero es de otra instancia") — de lo contrario un
    // comentario de la instancia 1 se podría tocar entrando por la URL de la 2.
    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_COMENTARIO_NORMAL_INSTANCIA')")
    public ComentarioNormalInstanciaResponseDTO editar(int codInstancia, int idComentario,
                                                           ComentarioNormalInstanciaRequestDTO dto) {
        ComentarioNormalInstancia comentario = buscarActivoOFallar(idComentario);
        validarPerteneceAInstancia(comentario, codInstancia);
        comentario.setContenido(dto.getContenido());
        // saveAndFlush: el UPDATE sale acá, así updatedAt ya viene con la fecha de esta edición.
        return aDto(comentarioRepository.saveAndFlush(comentario));
    }

    // ELIMINADO (no INACTIVO): a diferencia de los catálogos (Carrera, Grupo,
    // etc.), ningún comentario tiene reactivar() — unificado con el estado que
    // ya usan los comentarios de Estudiante (ComentarioNormalEstudiante/
    // ComentarioConfidencialEstudiante.eliminar()).
    @Transactional
    @PreAuthorize("hasAuthority('ELIMINAR_COMENTARIO_NORMAL_INSTANCIA')")
    public void eliminar(int codInstancia, int idComentario) {
        ComentarioNormalInstancia comentario = buscarActivoOFallar(idComentario);
        validarPerteneceAInstancia(comentario, codInstancia);
        comentario.setEstado(EstadoLogico.ELIMINADO);
        comentarioRepository.save(comentario);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ComentarioNormalInstanciaResponseDTO aDto(ComentarioNormalInstancia comentario) {
        ComentarioNormalInstanciaResponseDTO dto = comentarioMapper.toDto(comentario);
        if (comentario.getEstado() == EstadoLogico.ELIMINADO) {
            dto.setContenido(null);
            return dto;
        }
        // Un comentario ACTIVO solo se actualiza al editarlo, así que updated_at posterior a created_at
        // alcanza para saber que fue editado, sin una columna aparte.
        dto.setEditado(comentario.getCreatedAt() != null && comentario.getUpdatedAt() != null
                && comentario.getUpdatedAt().isAfter(comentario.getCreatedAt().plus(MARGEN_EDITADO)));
        return dto;
    }

    private void validarPerteneceAInstancia(ComentarioNormalInstancia comentario, int codInstancia) {
        if (comentario.getInstancia().getCodInstancia() != codInstancia) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No se encontró el comentario " + comentario.getIdComentario()
                            + " en la instancia " + codInstancia);
        }
    }

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