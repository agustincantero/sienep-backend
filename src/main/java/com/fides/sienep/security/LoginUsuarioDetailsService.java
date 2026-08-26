package com.fides.sienep.security;

import com.fides.sienep.model.*;
import com.fides.sienep.repository.EstudianteRepository;
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
import java.util.Optional;

/**
 * Carga el usuario por EMAIL.
 * Solo lo usa el AuthenticationManager durante el login
 * para validar email + contraseña.
 * Soporta tanto Funcionarios como Estudiantes.
 */
@Service
@RequiredArgsConstructor
public class LoginUsuarioDetailsService implements UserDetailsService {

    private final FuncionarioRepository funcionarioRepository;
    private final EstudianteRepository estudianteRepository;

    // noRollbackFor: sin esto, cuando una cuenta INACTIVO tira UsernameNotFoundException
    // acá, la transacción (compartida con AuthService.login(), que sí declara
    // noRollbackFor = ResponseStatusException.class) ya queda marcada rollback-only
    // en este punto — y aunque login() después arme un 401 prolijo, Spring tira
    // UnexpectedRollbackException al comitear y ese 401 termina como un 500 genérico.
    @Override
    @Transactional(readOnly = true, noRollbackFor = UsernameNotFoundException.class)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        // 1. Buscar como Funcionario
        Optional<Funcionario> funcionarioOpt = funcionarioRepository.findByEmail(email);
        if (funcionarioOpt.isPresent()) {
            Funcionario funcionario = funcionarioOpt.get();
            if (funcionario.getEstado() != EstadoLogico.ACTIVO && funcionario.getEstado() != EstadoLogico.PENDIENTE_DE_ACTIVACION) {
                throw new UsernameNotFoundException("Funcionario inactivo: " + email);
            }

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + funcionario.getRol().getNombre()));
            for (RolPermiso rp : funcionario.getRol().getRolPermisos()) {
                if (rp.getEstado() == EstadoLogico.ACTIVO) {
                    authorities.add(new SimpleGrantedAuthority(rp.getPermiso().getNombre()));
                }
            }

            return User.withUsername(email)
                    .password(funcionario.getContrasenia())
                    .authorities(authorities)
                    .build();
        }

        // 2. Si no es Funcionario, buscar como Estudiante
        Estudiante estudiante = estudianteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + email));

        if (estudiante.getEstado() != EstadoLogico.ACTIVO && estudiante.getEstado() != EstadoLogico.PENDIENTE_DE_ACTIVACION) {
            throw new UsernameNotFoundException("Estudiante inactivo: " + email);
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + estudiante.getRol().getNombre()));
        for (RolPermiso rp : estudiante.getRol().getRolPermisos()) {
            if (rp.getEstado() == EstadoLogico.ACTIVO) {
                authorities.add(new SimpleGrantedAuthority(rp.getPermiso().getNombre()));
            }
        }

        return User.withUsername(email)
                .password(estudiante.getContrasenia())
                .authorities(authorities)
                .build();
    }
}