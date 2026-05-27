package com.fides.sienep.service;

import com.fides.sienep.dto.RolDTO;
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
    public RolDTO toDTO(Rol rol) {
        RolDTO dto = new RolDTO();
        dto.setIdRol(rol.getIdRol());
        dto.setNombre(rol.getNombre());
        dto.setDescripcion(rol.getDescripcion());
        dto.setEstado(rol.getEstado());

        // Mapear solo los IDs de los permisos activos
        if (rol.getRolPermisos() != null) {
            List<Integer> permisosIds = rol.getRolPermisos().stream()
                    .filter(rp -> rp.getEstado() == EstadoLogico.ACTIVO)
                    .map(rp -> rp.getPermiso().getIdPermiso())
                    .collect(Collectors.toList());
            dto.setPermisosIds(permisosIds);
        } else {
            dto.setPermisosIds(new ArrayList<>());
        }
        return dto;
    }

    // --- Métodos CRUD ---
    @Transactional(readOnly = true)
    public List<RolDTO> obtenerTodos() {
        return rolRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RolDTO obtenerPorId(int id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        return toDTO(rol);
    }

    @Transactional
    public RolDTO crearRol(RolDTO rolDTO) {
        if (rolRepository.existsByNombre(rolDTO.getNombre())) {
            throw new RuntimeException("El nombre del rol ya existe");
        }

        Rol rol = new Rol();
        rol.setNombre(rolDTO.getNombre());
        rol.setDescripcion(rolDTO.getDescripcion());
        rol.setEstado(rolDTO.getEstado() != null ? rolDTO.getEstado() : EstadoLogico.ACTIVO);

        // Asignar permisos si vienen en el DTO
        if (rolDTO.getPermisosIds() != null && !rolDTO.getPermisosIds().isEmpty()) {
            for (Integer permisoId : rolDTO.getPermisosIds()) {
                // Usamos EntityManager.getReference para no hacer un SELECT innecesario
                Permiso permisoRef = entityManager.getReference(Permiso.class, permisoId);
                rol.agregarPermiso(permisoRef);
            }
        }

        Rol rolGuardado = rolRepository.save(rol);
        return toDTO(rolGuardado);
    }

    @Transactional
    public RolDTO actualizarRol(int id, RolDTO rolDTO) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        // Validar nombre único si lo están cambiando
        if (!rol.getNombre().equals(rolDTO.getNombre()) && rolRepository.existsByNombre(rolDTO.getNombre())) {
            throw new RuntimeException("El nombre del rol ya existe");
        }

        rol.setNombre(rolDTO.getNombre());
        rol.setDescripcion(rolDTO.getDescripcion());
        rol.setEstado(rolDTO.getEstado());

        // Lógica para actualizar permisos (Manejo de altas y bajas lógicas)
        if (rolDTO.getPermisosIds() != null) {
            List<Integer> nuevosPermisosIds = rolDTO.getPermisosIds();

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
        return toDTO(rolActualizado);
    }

    @Transactional
    public void darDeBajaRol(int id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        
        rol.setEstado(EstadoLogico.INACTIVO);
        rolRepository.save(rol);
    }
}
