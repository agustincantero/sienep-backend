package com.fides.sienep.service;

import com.fides.sienep.dto.PermisoResponseDTO;
import com.fides.sienep.dto.PermisoUpdateDTO;
import com.fides.sienep.model.Permiso;
import com.fides.sienep.repository.PermisoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import com.fides.sienep.mapper.PermisoMapper;
import com.fides.sienep.model.EstadoLogico;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermisoService {

    private final PermisoRepository permisoRepository;
    private final PermisoMapper mapper;

    @PreAuthorize("hasAuthority('VER_PERMISOS')")
    @Transactional(readOnly = true)
    @Cacheable("permisos")
    public List<PermisoResponseDTO> listar() {
        List<Permiso> permisos = permisoRepository.findAll();
        return mapper.toDtoList(permisos);
    }

    @PreAuthorize("hasAuthority('EDITAR_PERMISO')")
    @Transactional
    @CacheEvict(value = "permisos", allEntries = true)
    public PermisoResponseDTO editarDescripcion(int id, PermisoUpdateDTO requestDTO) {
        Permiso permiso = permisoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permiso no encontrado"));

        if (permiso.getEstado() == EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede editar un permiso inactivo");
        }

        permiso.setDescripcion(requestDTO.getDescripcion());
        
        return mapper.toDto(permisoRepository.save(permiso));
    }

}
