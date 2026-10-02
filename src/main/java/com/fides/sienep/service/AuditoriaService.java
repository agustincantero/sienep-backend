package com.fides.sienep.service;

import com.fides.sienep.model.Auditoria;
import com.fides.sienep.model.TipoAccion;
import com.fides.sienep.model.Usuario;
import com.fides.sienep.repository.AuditoriaRepository;
import com.fides.sienep.repository.UsuarioRepository;
import com.fides.sienep.dto.AuditoriaResponseDTO;
import com.fides.sienep.mapper.AuditoriaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
@Slf4j
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaMapper mapper;

    // REQUIRES_NEW: si la auditoría falla no debe dejar rollback-only la transacción de quien la llama (p. ej. logout, que invalida el token en esa misma transacción)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEventoManual(String entidad, TipoAccion accion, Integer idUsuario, String ip) {
        Usuario autor = usuarioRepository.findById(idUsuario).orElse(null);
        if (autor == null) {
            log.warn("Intento fallido de auditoría: No se encontró al usuario con ID {} para la acción {} en la entidad {}", idUsuario, accion, entidad);
            return;
        }

        Auditoria auditoria = Auditoria.builder()
                .entidad(entidad.toUpperCase())
                .accion(accion)
                .autor(autor)
                .ip(ip)
                .build();

        auditoriaRepository.save(auditoria);
    }

    @PreAuthorize("hasAuthority('VER_AUDITORIA')")
    @Transactional(readOnly = true)
    public Page<AuditoriaResponseDTO> listarAuditorias(int page, int size, String entidad, TipoAccion accion, Integer idAutor, String texto) {
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
            // Búsqueda general por autor (nombre, apellido o documento, parcial e insensible a mayúsculas), mismo criterio que EstudianteRepository.buscarPorTexto. Un evento sin autor (cambio directo en BD) no tiene nada contra qué buscar, así que queda afuera cuando se usa este filtro.
            if (texto != null && !texto.isBlank()) {
                String comodin = "%" + texto.toLowerCase() + "%";
                predicates.add(cb.and(
                        cb.isNotNull(root.get("autor")),
                        cb.or(
                                cb.like(cb.lower(root.get("autor").get("nombre")), comodin),
                                cb.like(cb.lower(root.get("autor").get("apellido")), comodin),
                                cb.like(cb.lower(root.get("autor").get("documento")), comodin)
                        )
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Auditoria> auditoriasPage = auditoriaRepository.findAll(spec, pageable);
        
        List<AuditoriaResponseDTO> respuestas = mapper.toDtoList(auditoriasPage.getContent());

        return new PageImpl<>(respuestas, pageable, auditoriasPage.getTotalElements());
    }

    @PreAuthorize("hasAuthority('VER_AUDITORIA')")
    @Transactional(readOnly = true)
    public AuditoriaResponseDTO obtenerAuditoriaPorId(int id) {
        Auditoria auditoria = auditoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auditoria no encontrada con el ID: " + id));
        return mapper.toDto(auditoria);
    }
}
