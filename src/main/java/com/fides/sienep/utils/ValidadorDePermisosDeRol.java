package com.fides.sienep.utils;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Rol;
import com.fides.sienep.model.RolPermiso;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

/**
 * Evita que un actor otorgue (a sí mismo o a otro) un rol con permisos que él
 * mismo no tiene — sin esto, cualquier rol personalizado (RF34) que incluya
 * CREAR_FUNCIONARIO/EDITAR_FUNCIONARIO/CREAR_ROL/EDITAR_ROL sin ser un admin
 * completo podría escalar a un rol con más privilegios.
 *
 * Usado por FuncionarioService (al asignar un rol a un funcionario) y RolService
 * (al definir qué permisos tiene un rol nuevo o editado).
 */
public final class ValidadorDePermisosDeRol {

    private ValidadorDePermisosDeRol() {
    }

    public static void validarActorPuedeAsignarRol(Rol rolDestino) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Set<String> permisosDelActor = new HashSet<>();
        for (GrantedAuthority authority : auth.getAuthorities()) {
            permisosDelActor.add(authority.getAuthority());
        }

        for (RolPermiso rp : rolDestino.getRolPermisos()) {
            if (rp.getEstado() != EstadoLogico.ACTIVO) {
                continue;
            }
            if (!permisosDelActor.contains(rp.getPermiso().getNombre())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No podés asignar un rol que tenga permisos que vos mismo no tenés.");
            }
        }
    }
}
