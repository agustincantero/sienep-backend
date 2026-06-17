package com.fides.sienep.service;

import com.fides.sienep.model.Auditoria;
import com.fides.sienep.model.TipoAccion;
import com.fides.sienep.model.Usuario;
import com.fides.sienep.repository.AuditoriaRepository;
import com.fides.sienep.repository.UsuarioRepository;
import com.fides.sienep.dto.AuditoriaResponseDTO;
import com.fides.sienep.mapper.AuditoriaMapper;
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
import org.springframework.security.access.prepost.PreAuthorize;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VER_AUDITORIA')")
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaMapper mapper;

    @Transactional
    public void registrarEventoManual(String entidad, TipoAccion accion, Integer idUsuario, String ip) {
        Usuario autor = usuarioRepository.findById(idUsuario).orElse(null);
        if (autor == null) return;

        Auditoria auditoria = Auditoria.builder()
                .entidad(entidad.toUpperCase())
                .accion(accion)
                .autor(autor)
                .ip(ip)
                .build();

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
        
        List<AuditoriaResponseDTO> respuestas = mapper.toDtoList(auditoriasPage.getContent());

        return new PageImpl<>(respuestas, pageable, auditoriasPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AuditoriaResponseDTO obtenerAuditoriaPorId(int id) {
        Auditoria auditoria = auditoriaRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Registro de auditoría no encontrado"));
        return mapper.toDto(auditoria);
    }
}
