package com.fides.sienep.service;

import com.fides.sienep.dto.RolCreateDTO;
import com.fides.sienep.dto.RolUpdateDTO;
import com.fides.sienep.dto.RolResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Permiso;
import com.fides.sienep.model.Rol;
import com.fides.sienep.model.RolPermiso;
import com.fides.sienep.repository.PermisoRepository;
import com.fides.sienep.repository.RolRepository;
import com.fides.sienep.utils.ValidadorDePermisosDeRol;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import com.fides.sienep.mapper.RolMapper;

import java.util.List;

/**
 * Servicio encargado de la gestión completa de los Roles.
 * 
 * <p><b>Mecanismo de Caché:</b></p>
 * <ul>
 *   <li>{@code @Cacheable("roles")}: Guarda el resultado de la consulta en la memoria RAM del servidor.
 *       Las siguientes peticiones se responden de forma instantánea sin ir a PostgreSQL.</li>
 *   <li>{@code @CacheEvict(value = "roles", allEntries = true)}: Actúa como la "destructora". 
 *       Cuando ocurre un alta, baja o modificación, esta anotación vacía la caja "roles" de la memoria RAM.
 *       Esto obliga al sistema a volver a consultar la base de datos la próxima vez, garantizando 
 *       que nunca se le muestren datos viejos a los usuarios.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final EntityManager entityManager;
    private final RolMapper mapper;

    // --- Validaciones Internas ---
    private void validarRolProtegido(Rol rol) {
        if ("ADMINISTRADOR".equalsIgnoreCase(rol.getNombre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acción denegada: No se puede modificar ni desactivar el rol base ADMINISTRADOR.");
        }
    }

    // --- Métodos CRUD ---
    @PreAuthorize("hasAuthority('VER_ROLES')")
    @Transactional(readOnly = true)
    @Cacheable("roles")
    public List<RolResponseDTO> obtenerTodos() {
        List<Rol> roles = rolRepository.findAll();
        return mapper.toDtoList(roles);
    }

    @PreAuthorize("hasAuthority('VER_ROLES')")
    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "#id")
    public RolResponseDTO obtenerPorId(int id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
        return mapper.toDto(rol);
    }

    @PreAuthorize("hasAuthority('CREAR_ROL')")
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
    public RolResponseDTO crearRol(RolCreateDTO requestDTO) {
        // Normalización del nombre
        String nombreNormalizado = requestDTO.getNombre().trim().toUpperCase().replaceAll("\\s+", "_");

        if (rolRepository.existsByNombre(nombreNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre del rol ya existe");
        }

        // Validar que los permisos existen en la BD
        if (requestDTO.getPermisosIds() != null && !requestDTO.getPermisosIds().isEmpty()) {
            long permisosExistentes = permisoRepository.countByIdPermisoIn(requestDTO.getPermisosIds());
            if (permisosExistentes != requestDTO.getPermisosIds().size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uno o más IDs de permisos proporcionados no existen en la base de datos");
            }
        }

        Rol rol = new Rol();
        rol.setNombre(nombreNormalizado);
        rol.setDescripcion(requestDTO.getDescripcion());
        // Forzamos ACTIVO siempre en la creación
        rol.setEstado(EstadoLogico.ACTIVO);

        // Asignar permisos si vienen en el DTO
        if (requestDTO.getPermisosIds() != null && !requestDTO.getPermisosIds().isEmpty()) {
            for (Integer permisoId : requestDTO.getPermisosIds()) {
                Permiso permisoRef = entityManager.getReference(Permiso.class, permisoId);
                rol.agregarPermiso(permisoRef);
            }
        }

        // El actor no puede crear un rol con permisos que él mismo no tiene (ver
        // ValidadorDePermisosDeRol) — blindaje para roles personalizados (RF34).
        ValidadorDePermisosDeRol.validarActorPuedeAsignarRol(rol);

        Rol rolGuardado = rolRepository.save(rol);
        return mapper.toDto(rolGuardado);
    }

    @PreAuthorize("hasAuthority('EDITAR_ROL')")
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
    public RolResponseDTO actualizarRol(int id, RolUpdateDTO requestDTO) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));

        validarRolProtegido(rol);

        if (rol.getEstado() == EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede editar un rol inactivo. Por favor, reactívelo primero.");
        }

        // Normalización del nombre
        String nombreNormalizado = requestDTO.getNombre().trim().toUpperCase().replaceAll("\\s+", "_");

        // Validar nombre único si lo están cambiando
        if (!rol.getNombre().equals(nombreNormalizado) && rolRepository.existsByNombre(nombreNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre del rol ya existe");
        }

        // Validar que los permisos existen en la BD
        if (requestDTO.getPermisosIds() != null && !requestDTO.getPermisosIds().isEmpty()) {
            long permisosExistentes = permisoRepository.countByIdPermisoIn(requestDTO.getPermisosIds());
            if (permisosExistentes != requestDTO.getPermisosIds().size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uno o más IDs de permisos proporcionados no existen en la base de datos");
            }
        }

        rol.setNombre(nombreNormalizado);
        rol.setDescripcion(requestDTO.getDescripcion());

        // Lógica para actualizar permisos (Manejo de altas y bajas lógicas)
        // Si el cliente envía una lista vacía ([]), significa que desea revocar (desactivación lógica) TODOS los permisos del rol.
        if (requestDTO.getPermisosIds() != null) {
            List<Integer> nuevosPermisosIds = requestDTO.getPermisosIds();

            // 1. Desactivar (INACTIVO) los permisos que ya no están en la lista
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

        // El actor no puede dejar el rol con permisos que él mismo no tiene (ver
        // ValidadorDePermisosDeRol) — blindaje para roles personalizados (RF34).
        ValidadorDePermisosDeRol.validarActorPuedeAsignarRol(rol);

        Rol rolActualizado = rolRepository.save(rol);
        return mapper.toDto(rolActualizado);
    }

    @PreAuthorize("hasAuthority('DESACTIVAR_ROL')")
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
    public void desactivarRol(int id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
        
        validarRolProtegido(rol);

        if (rol.getEstado() == EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El rol ya se encuentra inactivo");
        }

        rol.setEstado(EstadoLogico.INACTIVO);
        rolRepository.save(rol);
    }

    @PreAuthorize("hasAuthority('REACTIVAR_ROL')")
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
    public RolResponseDTO reactivarRol(int id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));

        if (rol.getEstado() == EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El rol ya se encuentra activo");
        }

        rol.setEstado(EstadoLogico.ACTIVO);
        return mapper.toDto(rolRepository.save(rol));
    }
}
