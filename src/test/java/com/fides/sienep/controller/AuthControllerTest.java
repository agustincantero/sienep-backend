package com.fides.sienep.controller;

import com.fides.sienep.dto.LoginRequestDTO;
import com.fides.sienep.dto.LoginResponseDTO;
import com.fides.sienep.dto.OlvideContraseniaDTO;
import com.fides.sienep.dto.RestablecerContraseniaConTokenDTO;
import com.fides.sienep.dto.UsuarioAutenticadoDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.TipoUsuario;
import com.fides.sienep.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// P1 de la auditoría — mismo criterio que CarreraControllerTest (ver ese archivo
// y AbstractControllerTest para el porqué del diseño). /auth/** es permitAll en
// SecurityConfig, así que estos tests no dependen de @WithMockUser para pasar.
@WebMvcTest(AuthController.class)
class AuthControllerTest extends AbstractControllerTest {

    @MockitoBean private AuthService authService;

    @Test
    @DisplayName("Login - Credenciales válidas responde 200 con el token")
    void login_Valido_Responde200() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("juan.garcia@utec.edu.uy");
        dto.setContrasenia("Admin123!");
        when(authService.login(any(), anyString())).thenReturn(new LoginResponseDTO("token-fake"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-fake"));
    }

    @Test
    @DisplayName("Login - Email con formato inválido responde 400 (Bean Validation)")
    void login_EmailInvalido_Responde400() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("no-es-un-email");
        dto.setContrasenia("Admin123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Login - Credenciales inválidas responde 401 (propagado desde el service)")
    void login_CredencialesInvalidas_Responde401() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("juan.garcia@utec.edu.uy");
        dto.setContrasenia("incorrecta");
        when(authService.login(any(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Olvidé mi contraseña - Email válido responde 200")
    void olvideMiContrasenia_Valido_Responde200() throws Exception {
        OlvideContraseniaDTO dto = new OlvideContraseniaDTO();
        dto.setEmail("juan.garcia@utec.edu.uy");

        mockMvc.perform(post("/auth/olvide-contrasenia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Olvidé mi contraseña - Email inválido responde 400 (Bean Validation)")
    void olvideMiContrasenia_EmailInvalido_Responde400() throws Exception {
        OlvideContraseniaDTO dto = new OlvideContraseniaDTO();
        dto.setEmail("no-es-un-email");

        mockMvc.perform(post("/auth/olvide-contrasenia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Restablecer contraseña - Token inválido responde 401 (propagado desde el service)")
    void restablecerContrasenia_TokenInvalido_Responde401() throws Exception {
        RestablecerContraseniaConTokenDTO dto = new RestablecerContraseniaConTokenDTO();
        dto.setToken("token-vencido");
        dto.setContraseniaNueva("NuevaPass123!");
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido o vencido"))
                .when(authService).restablecerContraseniaConToken(anyString(), anyString());

        mockMvc.perform(post("/auth/restablecer-contrasenia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Restablecer contraseña - Contraseña nueva demasiado corta responde 400 (Bean Validation)")
    void restablecerContrasenia_ContraseniaCorta_Responde400() throws Exception {
        RestablecerContraseniaConTokenDTO dto = new RestablecerContraseniaConTokenDTO();
        dto.setToken("token-valido");
        dto.setContraseniaNueva("corta");

        mockMvc.perform(post("/auth/restablecer-contrasenia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Me - Autenticado (sin permiso específico) responde 200 con identidad y permisos")
    void me_Autenticado_Responde200() throws Exception {
        UsuarioAutenticadoDTO dto = UsuarioAutenticadoDTO.builder()
                .idUsuario(12)
                .tipo(TipoUsuario.FUNCIONARIO)
                .email("ana.perez@utec.edu.uy")
                .nombre("Ana")
                .apellido("Pérez")
                .estado(EstadoLogico.ACTIVO)
                .rol("ADMINISTRADOR")
                .permisos(List.of("CREAR_INSTANCIA", "VER_FUNCIONARIOS"))
                .build();
        when(authService.usuarioAutenticado()).thenReturn(dto);

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(12))
                .andExpect(jsonPath("$.tipo").value("FUNCIONARIO"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.permisos[0]").value("CREAR_INSTANCIA"))
                .andExpect(jsonPath("$.permisos.length()").value(2));
    }

    @Test
    @DisplayName("Me - Error propagado desde el service responde 401")
    void me_UsuarioDelTokenNoExiste_Responde401() throws Exception {
        when(authService.usuarioAutenticado())
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario del token no encontrado"));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
