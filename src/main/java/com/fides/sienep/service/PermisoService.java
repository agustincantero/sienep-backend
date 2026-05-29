package com.fides.sienep.service;

import com.fides.sienep.dto.PermisoUpdateDTO;
import com.fides.sienep.model.Permiso;
import com.fides.sienep.repository.PermisoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermisoService {

    private final PermisoRepository permisoRepository;

    @Autowired
    public PermisoService(PermisoRepository permisoRepository) {
        this.permisoRepository = permisoRepository;
    }

    @Transactional(readOnly = true)
    public List<Permiso> obtenerTodos() {
        return permisoRepository.findAll();
    }

    @Transactional
    public Permiso actualizarDescripcion(int id, PermisoUpdateDTO requestDTO) {
        Permiso permiso = permisoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permiso no encontrado"));

        // SOLO actualizamos la descripción. El nombre y el estado quedan bloqueados.
        permiso.setDescripcion(requestDTO.getDescripcion());
        
        return permisoRepository.save(permiso);
    }
}
