package com.fides.sienep.controller;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Permiso;
import com.fides.sienep.model.Rol;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.utils.JwtTokenUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Confirma el enforcement de JwtRequestFilter para un usuario PENDIENTE_DE_ACTIVACION
// contra el SecurityConfig/filtro real de punta a punta — algo que no se puede
// verificar con @WebMvcTest (no carga el SecurityFilterChain real). Mismo patrón que
// AuthMeControllerTest: JWT y MockMvc reales, solo repositorios mockeados.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PendienteActivacionEnforcementTest {

    private static final int ID_FUNCIONARIO = 3;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenUtil jwtTokenUtil;

    @MockitoBean private FuncionarioRepository funcionarioRepository;
    @MockitoBean private EstudianteRepository estudianteRepository;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    // El rol lleva VER_ROLES a propósito: si el enforcement de JwtRequestFilter se
    // rompiera, este funcionario SÍ tendría permiso para listar roles y el test de
    // "responde 403" pasaría a fallar con 200 en vez de esconder la regresión detrás
    // de un 403 casual por falta de permiso.
    private String tokenFuncionarioPendienteDeActivacion() {
        Rol rol = new Rol("ADMINISTRADOR");
        rol.agregarPermiso(new Permiso("VER_ROLES", "descripcion de prueba"));

        Funcionario funcionario = new Funcionario();
        funcionario.setIdUsuario(ID_FUNCIONARIO);
        funcionario.setNombre("Beatriz");
        funcionario.setApellido("Gómez");
        funcionario.setEmail("beatriz.gomez@utec.edu.uy");
        funcionario.setEstado(EstadoLogico.PENDIENTE_DE_ACTIVACION);
        funcionario.setRol(rol);

        // findById: lo usa CustomUsuarioDetailsService (JwtRequestFilter).
        // findByIdConRolYPermisos: lo usa AuthService.usuarioAutenticado() (/auth/me).
        when(funcionarioRepository.findById(ID_FUNCIONARIO)).thenReturn(Optional.of(funcionario));
        when(funcionarioRepository.findByIdConRolYPermisos(ID_FUNCIONARIO)).thenReturn(Optional.of(funcionario));

        return jwtTokenUtil.generateToken(ID_FUNCIONARIO, rol.getNombre());
    }

    @Test
    @DisplayName("PENDIENTE_DE_ACTIVACION - endpoint fuera de la whitelist responde 403")
    void pendienteDeActivacion_EndpointFueraDeWhitelist_Responde403() throws Exception {
        String jwt = tokenFuncionarioPendienteDeActivacion();

        mockMvc.perform(get("/roles").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PENDIENTE_DE_ACTIVACION - GET /auth/me (whitelisteado) responde 200")
    void pendienteDeActivacion_AuthMe_Responde200() throws Exception {
        String jwt = tokenFuncionarioPendienteDeActivacion();

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_DE_ACTIVACION"));
    }
}
