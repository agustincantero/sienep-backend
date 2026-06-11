package com.fides.sienep.security;

import com.fides.sienep.model.*;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.RolRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
@Service
@RequiredArgsConstructor
public class CustomUsuarioDetailsService implements UserDetailsService {

    private final FuncionarioRepository funcionarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

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

        // 1. Buscar como Funcionario
        Optional<Funcionario> funcionarioOpt = funcionarioRepository.findById(idUsuario);
        if (funcionarioOpt.isPresent()) {
            return buildFuncionarioDetails(funcionarioOpt.get());
        }

        // 2. Si no es Funcionario, buscar como Estudiante
        Estudiante estudiante = estudianteRepository.findById(idUsuario)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con id: " + idUsuario));

        return buildEstudianteDetails(estudiante);
    }

    private UserDetails buildFuncionarioDetails(Funcionario funcionario) {
        if (funcionario.getEstado() != EstadoLogico.ACTIVO) {
            throw new UsernameNotFoundException("Funcionario inactivo: " + funcionario.getIdUsuario());
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + funcionario.getRol().getNombre()));

        for (RolPermiso rp : funcionario.getRol().getRolPermisos()) {
            if (rp.getEstado() == EstadoLogico.ACTIVO) {
                authorities.add(new SimpleGrantedAuthority(rp.getPermiso().getNombre()));
            }
        }

        return User.withUsername(String.valueOf(funcionario.getIdUsuario()))
                .password(funcionario.getContrasenia())
                .authorities(authorities)
                .build();
    }

    private UserDetails buildEstudianteDetails(Estudiante estudiante) {
        if (estudiante.getEstado() != EstadoLogico.ACTIVO) {
            throw new UsernameNotFoundException("Estudiante inactivo: " + estudiante.getIdUsuario());
        }

        // Rol sintético — los estudiantes no tienen rol en BD
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"));
        authorities.add(new SimpleGrantedAuthority("VER_COMENTARIO_NORMAL_ESTUDIANTE"));

        return User.withUsername(String.valueOf(estudiante.getIdUsuario()))
                .password(estudiante.getContrasenia())
                .authorities(authorities)
                .build();
    }

    @PostConstruct
    @Transactional
    public void initUsuarios() {
        crearSiNoExiste("admin@sienep.com",   "Admin",   "Sistema", "00000000", "Admin123!",   "ADMINISTRADOR");
        crearSiNoExiste("tutor@sienep.com",   "Tutor",   "Base",    "11111111", "Tutor123!",   "TUTOR");
        crearSiNoExiste("psico@sienep.com",   "Psicop",  "Base",    "22222222", "Psico123!",   "PSICOPEDAGOGO");
        crearSiNoExiste("legales@sienep.com", "Legales", "Base",    "33333333", "Legales123!", "LEGALES");
        crearSiNoExiste("docente@sienep.com", "Docente", "Base",    "44444444", "Docente123!", "DOCENTE");
    }

    private void crearSiNoExiste(String email, String nombre, String apellido,
                                 String documento, String contrasenia, String nomRol) {
        if (funcionarioRepository.findByEmail(email).isPresent()) return;

        Rol rol = rolRepository.findByNombre(nomRol)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + nomRol));

        Funcionario funcionario = new Funcionario();
        funcionario.setNombre(nombre);
        funcionario.setApellido(apellido);
        funcionario.setDocumento(documento);
        funcionario.setPaisDocumento(PaisDocumento.URY);
        funcionario.setEmail(email);
        funcionario.setContrasenia(passwordEncoder.encode(contrasenia));
        funcionario.setEstado(EstadoLogico.ACTIVO);
        funcionario.setRol(rol);

        funcionarioRepository.save(funcionario);
    }
}