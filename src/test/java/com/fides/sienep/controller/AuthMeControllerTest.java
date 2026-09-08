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

// Cubre el matcher de SecurityConfig (`/auth/me` -> authenticated(), antes del
// `/auth/**` permitAll): eso NO se puede verificar con @WebMvcTest, que no carga
// el SecurityConfig real. Mismo patrón que ComentarioConfidencialInstanciaControllerTest
// (contexto completo, solo repos mockeados, JWT reales vía JwtTokenUtil).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthMeControllerTest {

    private static final int ID_FUNCIONARIO = 2;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenUtil jwtTokenUtil;

    @MockitoBean private FuncionarioRepository funcionarioRepository;
    @MockitoBean private EstudianteRepository estudianteRepository;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private String tokenFuncionario(String... permisos) {
        Rol rol = new Rol("ADMINISTRADOR");
        for (String p : permisos) {
            rol.agregarPermiso(new Permiso(p, "descripcion de prueba"));
        }
        Funcionario funcionario = new Funcionario();
        funcionario.setIdUsuario(ID_FUNCIONARIO);
        funcionario.setNombre("Ana");
        funcionario.setApellido("Pérez");
        funcionario.setEmail("ana.perez@utec.edu.uy");
        funcionario.setEstado(EstadoLogico.ACTIVO);
        funcionario.setRol(rol);

        // findById: lo usa CustomUsuarioDetailsService (JwtRequestFilter).
        // findByIdConRolYPermisos: lo usa AuthService.usuarioAutenticado().
        when(funcionarioRepository.findById(ID_FUNCIONARIO)).thenReturn(Optional.of(funcionario));
        when(funcionarioRepository.findByIdConRolYPermisos(ID_FUNCIONARIO)).thenReturn(Optional.of(funcionario));

        return jwtTokenUtil.generateToken(ID_FUNCIONARIO, rol.getNombre());
    }

    @Test
    @DisplayName("GET /auth/me - Sin header Authorization responde 401")
    void me_SinToken_Responde401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /auth/me - Token malformado responde 401")
    void me_TokenMalformado_Responde401() throws Exception {
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /auth/me - Token válido responde 200 con tipo, rol y permisos")
    void me_TokenValido_Responde200() throws Exception {
        String token = tokenFuncionario("CREAR_INSTANCIA", "VER_FUNCIONARIOS");

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(ID_FUNCIONARIO))
                .andExpect(jsonPath("$.tipo").value("FUNCIONARIO"))
                .andExpect(jsonPath("$.email").value("ana.perez@utec.edu.uy"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.permisos.length()").value(2))
                .andExpect(jsonPath("$.permisos[0]").value("CREAR_INSTANCIA"));
    }
}
