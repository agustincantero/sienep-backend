package com.fides.sienep.config;

import com.fides.sienep.model.*;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.RolRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Profile("!test")   // ← clave: no se ejecuta en el perfil test
public class DataSeeder {

    private final FuncionarioRepository funcionarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

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