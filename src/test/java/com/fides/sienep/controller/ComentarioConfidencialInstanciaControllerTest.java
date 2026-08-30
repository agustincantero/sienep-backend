package com.fides.sienep.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Instancia;
import com.fides.sienep.model.Permiso;
import com.fides.sienep.model.Rol;
import com.fides.sienep.repository.ComentarioConfidencialInstanciaRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.InstanciaRepository;
import com.fides.sienep.utils.JwtTokenUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Mismo criterio que ComentarioConfidencialEstudianteControllerTest — ver el
// comentario de esa clase para el porqué de @SpringBootTest en vez de @WebMvcTest.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComentarioConfidencialInstanciaControllerTest {

    private static final int ID_FUNCIONARIO = 2;
    private static final int COD_INSTANCIA = 1;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenUtil jwtTokenUtil;

    // Ver nota en ComentarioConfidencialEstudianteControllerTest: no hay un bean
    // ObjectMapper autoconfigurado para inyectar en este contexto de test, aunque
    // la serialización real de las responses sí funciona (la ejerce este mismo test).
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean private FuncionarioRepository funcionarioRepository;
    @MockitoBean private InstanciaRepository instanciaRepository;
    @MockitoBean private ComentarioConfidencialInstanciaRepository comentarioRepository;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private String tokenConPermisos(String... permisos) {
        Rol rol = new Rol("TUTOR");
        for (String permiso : permisos) {
            rol.agregarPermiso(new Permiso(permiso, "descripcion de prueba"));
        }

        Funcionario funcionario = new Funcionario();
        funcionario.setIdUsuario(ID_FUNCIONARIO);
        funcionario.setNombre("Juan");
        funcionario.setApellido("García");
        funcionario.setEstado(EstadoLogico.ACTIVO);
        funcionario.setRol(rol);

        when(funcionarioRepository.findById(ID_FUNCIONARIO)).thenReturn(Optional.of(funcionario));

        return jwtTokenUtil.generateToken(ID_FUNCIONARIO, rol.getNombre());
    }

    private String basePath() {
        return "/instancias/" + COD_INSTANCIA + "/comentarios-confidenciales";
    }

    @Test
    @DisplayName("Listar - Sin header Authorization responde 401")
    void listar_SinToken_Responde401() throws Exception {
        mockMvc.perform(get(basePath()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Listar - Con token pero sin el permiso responde 403")
    void listar_SinPermiso_Responde403() throws Exception {
        String token = tokenConPermisos("ALGUN_OTRO_PERMISO");
        when(instanciaRepository.findById(COD_INSTANCIA)).thenReturn(Optional.of(mock(Instancia.class)));

        mockMvc.perform(get(basePath()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Listar - Con el permiso responde 200")
    void listar_ConPermiso_Responde200() throws Exception {
        String token = tokenConPermisos("VER_COMENTARIO_CONFIDENCIAL_INSTANCIA");
        when(instanciaRepository.findById(COD_INSTANCIA)).thenReturn(Optional.of(mock(Instancia.class)));
        when(comentarioRepository.findByInstancia_CodInstanciaAndEstado(
                eq(COD_INSTANCIA), eq(EstadoLogico.ACTIVO), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get(basePath()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Crear - Sin el permiso responde 403")
    void crear_SinPermiso_Responde403() throws Exception {
        String token = tokenConPermisos("ALGUN_OTRO_PERMISO");
        when(instanciaRepository.findById(COD_INSTANCIA)).thenReturn(Optional.of(mock(Instancia.class)));

        mockMvc.perform(post(basePath())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonMap("contenido", "Comentario de prueba"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Crear - Con el permiso y datos válidos responde 201")
    void crear_ConPermiso_Responde201() throws Exception {
        String token = tokenConPermisos("CREAR_COMENTARIO_CONFIDENCIAL_INSTANCIA");
        when(instanciaRepository.findById(COD_INSTANCIA)).thenReturn(Optional.of(mock(Instancia.class)));
        when(comentarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post(basePath())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonMap("contenido", "Comentario de prueba"))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Crear - Contenido en blanco responde 400 (Bean Validation real)")
    void crear_ContenidoEnBlanco_Responde400() throws Exception {
        String token = tokenConPermisos("CREAR_COMENTARIO_CONFIDENCIAL_INSTANCIA");

        mockMvc.perform(post(basePath())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonMap("contenido", "   "))))
                .andExpect(status().isBadRequest());
    }
}
