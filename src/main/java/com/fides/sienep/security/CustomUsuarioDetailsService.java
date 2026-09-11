package com.fides.sienep.security;

import com.fides.sienep.model.*;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Carga el usuario por ID (subject del token JWT).
 * Usado por JwtRequestFilter en cada request autenticado.
 * Soporta tanto Funcionarios como Estudiantes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUsuarioDetailsService implements UserDetailsService {

    private final FuncionarioRepository funcionarioRepository;
    private final EstudianteRepository estudianteRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String idUsuarioStr)
            throws UsernameNotFoundException {

        int idUsuario;
        try {
            idUsuario = Integer.parseInt(idUsuarioStr);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("ID de usuario inválido: " + idUsuarioStr);
        }

        Optional<Funcionario> funcionarioOpt = funcionarioRepository.findById(idUsuario);
        if (funcionarioOpt.isPresent()) {
            UserDetails ud = buildFuncionarioDetails(funcionarioOpt.get());
            log.info("[DEBUG] Authorities para {}: {}", idUsuario, ud.getAuthorities());
            return ud;
        }

        Estudiante estudiante = estudianteRepository.findById(idUsuario)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con id: " + idUsuario));

        return buildEstudianteDetails(estudiante);
    }

    private UserDetails buildFuncionarioDetails(Funcionario funcionario) {
        if (funcionario.getEstado() != EstadoLogico.ACTIVO && funcionario.getEstado() != EstadoLogico.PENDIENTE_DE_ACTIVACION) {
            throw new UsernameNotFoundException("Funcionario inactivo: " + funcionario.getIdUsuario());
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + funcionario.getRol().getNombre()));

        for (RolPermiso rp : funcionario.getRol().getRolPermisos()) {
            if (rp.getEstado() == EstadoLogico.ACTIVO) {
                authorities.add(new SimpleGrantedAuthority(rp.getPermiso().getNombre()));
            }
        }

        return new SienepUserDetails(String.valueOf(funcionario.getIdUsuario()),
                funcionario.getContrasenia(), authorities, funcionario.getCredencialesVigentesDesde(),
                funcionario.getEstado());
    }

    private UserDetails buildEstudianteDetails(Estudiante estudiante) {
        if (estudiante.getEstado() != EstadoLogico.ACTIVO && estudiante.getEstado() != EstadoLogico.PENDIENTE_DE_ACTIVACION) {
            throw new UsernameNotFoundException("Estudiante inactivo: " + estudiante.getIdUsuario());
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + estudiante.getRol().getNombre()));
        for (RolPermiso rp : estudiante.getRol().getRolPermisos()) {
            if (rp.getEstado() == EstadoLogico.ACTIVO) {
                authorities.add(new SimpleGrantedAuthority(rp.getPermiso().getNombre()));
            }
        }

        return new SienepUserDetails(String.valueOf(estudiante.getIdUsuario()),
                estudiante.getContrasenia(), authorities, estudiante.getCredencialesVigentesDesde(),
                estudiante.getEstado());
    }
}