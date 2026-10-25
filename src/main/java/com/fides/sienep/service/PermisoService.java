package com.fides.sienep.service;

import com.fides.sienep.dto.PermisoResponseDTO;
import com.fides.sienep.model.Permiso;
import com.fides.sienep.repository.PermisoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import com.fides.sienep.mapper.PermisoMapper;

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

}
