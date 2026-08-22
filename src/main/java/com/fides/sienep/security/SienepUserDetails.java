package com.fides.sienep.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.time.LocalDateTime;
import java.util.Collection;

// Extiende el User estándar de Spring Security solo para poder llevar
// credencialesVigentesDesde hasta JwtTokenUtil.validateToken() — es lo único que
// CustomUsuarioDetailsService necesita agregar por encima del UserDetails de base.
public class SienepUserDetails extends User {

    private final LocalDateTime credencialesVigentesDesde;

    public SienepUserDetails(String username, String password,
                              Collection<? extends GrantedAuthority> authorities,
                              LocalDateTime credencialesVigentesDesde) {
        super(username, password, authorities);
        this.credencialesVigentesDesde = credencialesVigentesDesde;
    }

    public LocalDateTime getCredencialesVigentesDesde() {
        return credencialesVigentesDesde;
    }
}
