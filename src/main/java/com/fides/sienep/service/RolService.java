package com.fides.sienep.service;

import com.fides.sienep.dto.PermisoResumenDTO;
import com.fides.sienep.dto.RolRequestDTO;
import com.fides.sienep.dto.RolResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Permiso;
import com.fides.sienep.model.Rol;
import com.fides.sienep.model.RolPermiso;
import com.fides.sienep.repository.RolRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RolService {

    private final RolRepository rolRepository;
    private final EntityManager entityManager;

    @Autowired
    public RolService(RolRepository rolRepository, EntityManager entityManager) {
        this.rolRepository = rolRepository;
        this.entityManager = entityManager;
    }

    // --- Mapeos Manuales ---
    public RolResponseDTO toResponseDTO(Rol rol) {
        RolResponseDTO dto = new RolResponseDTO();
        dto.setIdRol(rol.getIdRol());
        dto.setNombre(rol.getNombre());
        dto.setDescripcion(rol.getDescripcion());
        dto.setEstado(rol.getEstado());

        // Mapear solo los permisos activos a DTOs de resumen
        if (rol.getRolPermisos() != null) {
            List<PermisoResumenDTO> permisosResumen = rol.getRolPermisos().stream()
                    .filter(rp -> rp.getEstado() == EstadoLogico.ACTIVO)
                    .map(rp -> {
                        PermisoResumenDTO permisoDTO = new PermisoResumenDTO();
                        permisoDTO.setIdPermiso(rp.getPermiso().getIdPermiso());
                        permisoDTO.setNombre(rp.getPermiso().getNombre());
                        return permisoDTO;
                    })
                    .collect(Collectors.toList());
            dto.setPermisos(permisosResumen);
        } else {
            dto.setPermisos(new ArrayList<>());
        }
        return dto;
    }

    // --- Métodos CRUD ---
    @Transactional(readOnly = true)
    public List<RolResponseDTO> obtenerTodos() {
        return rolRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RolResponseDTO obtenerPorId(int id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        return toResponseDTO(rol);
    }

    @Transactional
    public RolResponseDTO crearRol(RolRequestDTO requestDTO) {
        if (rolRepository.existsByNombre(requestDTO.getNombre())) {
            throw new RuntimeException("El nombre del rol ya existe");
        }

        Rol rol = new Rol();
        rol.setNombre(requestDTO.getNombre());
        rol.setDescripcion(requestDTO.getDescripcion());
        rol.setEstado(requestDTO.getEstado() != null ? requestDTO.getEstado() : EstadoLogico.ACTIVO);

        // Asignar permisos si vienen en el DTO
        if (requestDTO.getPermisosIds() != null && !requestDTO.getPermisosIds().isEmpty()) {
            for (Integer permisoId : requestDTO.getPermisosIds()) {
                Permiso permisoRef = entityManager.getReference(Permiso.class, permisoId);
                rol.agregarPermiso(permisoRef);
            }
        }

        Rol rolGuardado = rolRepository.save(rol);
        return toResponseDTO(rolGuardado);
    }

    @Transactional
    public RolResponseDTO actualizarRol(int id, RolRequestDTO requestDTO) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        // Validar nombre único si lo están cambiando
        if (!rol.getNombre().equals(requestDTO.getNombre()) && rolRepository.existsByNombre(requestDTO.getNombre())) {
            throw new RuntimeException("El nombre del rol ya existe");
        }

        rol.setNombre(requestDTO.getNombre());
        rol.setDescripcion(requestDTO.getDescripcion());
        rol.setEstado(requestDTO.getEstado());

        // Lógica para actualizar permisos (Manejo de altas y bajas lógicas)
        if (requestDTO.getPermisosIds() != null) {
            List<Integer> nuevosPermisosIds = requestDTO.getPermisosIds();

            // 1. Dar de baja (INACTIVO) a los permisos que ya no están en la lista
            for (RolPermiso rp : rol.getRolPermisos()) {
                if (rp.getEstado() == EstadoLogico.ACTIVO && !nuevosPermisosIds.contains(rp.getPermiso().getIdPermiso())) {
                    rp.setEstado(EstadoLogico.INACTIVO);
                }
            }

            // 2. Dar de alta los nuevos permisos
            for (Integer permisoId : nuevosPermisosIds) {
                boolean existe = false;
                for (RolPermiso rp : rol.getRolPermisos()) {
                    if (rp.getPermiso().getIdPermiso() == permisoId) {
                        existe = true;
                        // Si estaba inactivo, lo reactivamos
                        if (rp.getEstado() != EstadoLogico.ACTIVO) {
                            rp.setEstado(EstadoLogico.ACTIVO);
                        }
                        break;
                    }
                }
                // Si no existía la relación, la creamos
                if (!existe) {
                    Permiso permisoRef = entityManager.getReference(Permiso.class, permisoId);
                    rol.agregarPermiso(permisoRef);
                }
            }
        }

        Rol rolActualizado = rolRepository.save(rol);
        return toResponseDTO(rolActualizado);
    }

    @Transactional
    public void darDeBajaRol(int id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        
        rol.setEstado(EstadoLogico.INACTIVO);
        rolRepository.save(rol);
    }
}
