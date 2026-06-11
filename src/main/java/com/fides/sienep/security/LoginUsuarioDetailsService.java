package com.fides.sienep.security;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.RolPermiso;
import com.fides.sienep.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Carga el usuario por EMAIL.
 * Solo lo usa el AuthenticationManager durante el login
 * para validar email + contraseña.
 */
@Service
@RequiredArgsConstructor
public class LoginUsuarioDetailsService implements UserDetailsService {

    private final FuncionarioRepository funcionarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Funcionario funcionario = funcionarioRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuario no encontrado: " + email));

        if (funcionario.getEstado() != EstadoLogico.ACTIVO) {
            throw new UsernameNotFoundException(
                    "Usuario inactivo o pendiente de activación");
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(
                "ROLE_" + funcionario.getRol().getNombre()));

        for (RolPermiso rp : funcionario.getRol().getRolPermisos()) {
            if (rp.getEstado() == EstadoLogico.ACTIVO) {
                authorities.add(new SimpleGrantedAuthority(
                        rp.getPermiso().getNombre()));
            }
        }

        // El username aquí es el email — solo para que authManager valide la contraseña
        return User.withUsername(email)
                .password(funcionario.getContrasenia())
                .authorities(authorities)
                .build();
    }
}