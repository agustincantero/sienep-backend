package com.fides.sienep.config;

import com.fides.sienep.model.*;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.RolRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Solo corre si SEED_DEMO_USERS=true está explícitamente seteado (ver
// application.properties). Default false: a diferencia de "@Profile(\"!test\")"
// (que corría en cualquier perfil salvo test, incluida producción si no se fijaba
// spring.profiles.active), acá el default seguro es NO crear usuarios, y hay que
// habilitarlo a mano en dev/local.
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-demo-users", havingValue = "true")
public class DataSeeder {

    private final FuncionarioRepository funcionarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

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
                .orElseThrow(() -> {
                    log.error("[AUDIT] DataSeeder: rol no encontrado al inicializar usuarios: {}", nomRol);
                    return new IllegalStateException("Rol no encontrado durante inicialización: " + nomRol);
                });

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