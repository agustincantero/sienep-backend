package com.fides.sienep.security;

import com.fides.sienep.model.EstadoLogico;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.time.LocalDateTime;
import java.util.Collection;

// Extiende el User estándar de Spring Security para poder llevar hasta el resto
// de la capa de seguridad datos que CustomUsuarioDetailsService ya resolvió y que
// un UserDetails de base no expone: credencialesVigentesDesde (usado por
// JwtTokenUtil.validateToken() para invalidar tokens viejos tras un cambio de
// contraseña) y estado (usado por JwtRequestFilter para restringir a un usuario
// PENDIENTE_DE_ACTIVACION a un puñado de endpoints hasta que elija una contraseña
// nueva).
public class SienepUserDetails extends User {

    private final LocalDateTime credencialesVigentesDesde;
    private final EstadoLogico estado;

    public SienepUserDetails(String username, String password,
                              Collection<? extends GrantedAuthority> authorities,
                              LocalDateTime credencialesVigentesDesde,
                              EstadoLogico estado) {
        super(username, password, authorities);
        this.credencialesVigentesDesde = credencialesVigentesDesde;
        this.estado = estado;
    }

    public LocalDateTime getCredencialesVigentesDesde() {
        return credencialesVigentesDesde;
    }

    public EstadoLogico getEstado() {
        return estado;
    }
}
