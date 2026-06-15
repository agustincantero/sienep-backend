package com.fides.sienep.service;

import com.fides.sienep.model.Auditoria;
import com.fides.sienep.model.TipoAccion;
import com.fides.sienep.model.Usuario;
import com.fides.sienep.repository.AuditoriaRepository;
import com.fides.sienep.repository.UsuarioRepository;
import com.fides.sienep.dto.AuditoriaResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void registrarEventoManual(String entidad, TipoAccion accion, Integer idUsuario, String ip) {
        Usuario autor = usuarioRepository.findById(idUsuario).orElse(null);
        if (autor == null) return;

        Auditoria auditoria = new Auditoria();
        auditoria.setEntidad(entidad.toUpperCase());
        auditoria.setAccion(accion);
        auditoria.setAutor(autor);
        auditoria.setIp(ip);
        auditoria.setValorAnterior(null);
        auditoria.setValorNuevo(null);

        auditoriaRepository.save(auditoria);
    }

    @Transactional(readOnly = true)
    public Page<AuditoriaResponseDTO> listarAuditorias(int page, int size, String entidad, TipoAccion accion, Integer idAutor) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaHora").descending());
        
        Specification<Auditoria> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entidad != null) {
                predicates.add(cb.equal(root.get("entidad"), entidad));
            }
            if (accion != null) {
                predicates.add(cb.equal(root.get("accion"), accion));
            }
            if (idAutor != null) {
                predicates.add(cb.equal(root.get("autor").get("idUsuario"), idAutor));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Auditoria> auditoriasPage = auditoriaRepository.findAll(spec, pageable);
        
        List<AuditoriaResponseDTO> respuestas = new ArrayList<>();

        for (Auditoria auditoria : auditoriasPage.getContent()) {
            respuestas.add(mapToResponseDTO(auditoria));
        }

        return new PageImpl<>(respuestas, pageable, auditoriasPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AuditoriaResponseDTO obtenerAuditoriaPorId(int id) {
        Auditoria auditoria = auditoriaRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Registro de auditoría no encontrado"));
        return mapToResponseDTO(auditoria);
    }

    private AuditoriaResponseDTO mapToResponseDTO(Auditoria auditoria) {
        String nombreAutor = null;
        Integer idAutor = null;

        if (auditoria.getAutor() != null) {
            idAutor = auditoria.getAutor().getIdUsuario();
            nombreAutor = auditoria.getAutor().getNombre() + " " + auditoria.getAutor().getApellido();
        }

        return AuditoriaResponseDTO.builder()
                .idAuditoria(auditoria.getId())
                .entidad(auditoria.getEntidad())
                .accion(auditoria.getAccion())
                .idAutor(idAutor)
                .nombreAutor(nombreAutor)
                .usuarioBd(auditoria.getUsuarioBd())
                .fechaHora(auditoria.getFechaHora())
                .ip(auditoria.getIp())
                .valorAnterior(auditoria.getValorAnterior() != null ? auditoria.getValorAnterior().toString() : null)
                .valorNuevo(auditoria.getValorNuevo() != null ? auditoria.getValorNuevo().toString() : null)
                .build();
    }
}
