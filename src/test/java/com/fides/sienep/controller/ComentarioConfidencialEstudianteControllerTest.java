package com.fides.sienep.controller;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Permiso;
import com.fides.sienep.model.Rol;
import com.fides.sienep.repository.ComentarioConfidencialEstudianteRepository;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.utils.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// P0 de la auditoría (sección 5): "Tests @WebMvcTest/MockMvc con filtro JWT real
// ... 403 sin permiso, 200 con permiso — end-to-end, no mockeado". Importante:
// acá NO se usa @WebMvcTest — ese slice reemplaza el service por un mock, y como
// @PreAuthorize vive en el SERVICE (no en el controller), un mock nunca ejecuta
// ese chequeo. Se usa @SpringBootTest + MockMvc con el contexto completo (service
// real, con su proxy de seguridad real) y solo los repositorios mockeados, para
// que la request HTTP atraviese de verdad: JwtRequestFilter → @PreAuthorize real
// → GlobalExceptionHandler. Es la única forma de que este test detecte, por
// ejemplo, un @PreAuthorize borrado por accidente.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComentarioConfidencialEstudianteControllerTest {

    private static final int ID_FUNCIONARIO = 2;
    private static final int ID_ESTUDIANTE = 5;
    private static final String BASE_PATH = "/comentarios-confidenciales-estudiante";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenUtil jwtTokenUtil;

    // Instancia propia, no autowireada: en este proyecto no hay un starter de
    // Jackson que autoconfigure un bean ObjectMapper (spring-boot-starter-webmvc
    // por sí solo no lo trae en Spring Boot 4) — no hace falta para este test,
    // que solo necesita serializar el body de la request, no ejercer el
    // ObjectMapper real que usa la app para las responses.
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean private FuncionarioRepository funcionarioRepository;
    @MockitoBean private EstudianteRepository estudianteRepository;
    @MockitoBean private ComentarioConfidencialEstudianteRepository comentarioRepository;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    // Registra el funcionario autenticado en el repositorio mockeado (lo consulta
    // tanto JwtRequestFilter vía CustomUsuarioDetailsService como el service al
    // resolver el autor) y devuelve un JWT real firmado con el secret de test.
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

    private Map<String, Object> bodyCrear(String contenido) {
        Map<String, Object> body = new HashMap<>();
        body.put("contenido", contenido);
        body.put("idEstudiante", ID_ESTUDIANTE);
        return body;
    }

    @Test
    @DisplayName("Listar - Sin header Authorization responde 401")
    void listar_SinToken_Responde401() throws Exception {
        mockMvc.perform(get(BASE_PATH).param("idEstudiante", String.valueOf(ID_ESTUDIANTE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Listar - Con token pero sin el permiso responde 403")
    void listar_SinPermiso_Responde403() throws Exception {
        String token = tokenConPermisos("ALGUN_OTRO_PERMISO");

        mockMvc.perform(get(BASE_PATH)
                        .param("idEstudiante", String.valueOf(ID_ESTUDIANTE))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Listar - Con el permiso responde 200")
    void listar_ConPermiso_Responde200() throws Exception {
        String token = tokenConPermisos("VER_COMENTARIO_CONFIDENCIAL_ESTUDIANTE");
        when(comentarioRepository.findByEstudianteIdUsuarioAndEstado(
                eq(ID_ESTUDIANTE), eq(EstadoLogico.ACTIVO), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get(BASE_PATH)
                        .param("idEstudiante", String.valueOf(ID_ESTUDIANTE))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Crear - Sin el permiso responde 403")
    void crear_SinPermiso_Responde403() throws Exception {
        String token = tokenConPermisos("ALGUN_OTRO_PERMISO");

        mockMvc.perform(post(BASE_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyCrear("Observación de prueba"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Crear - Con el permiso y datos válidos responde 201")
    void crear_ConPermiso_Responde201() throws Exception {
        String token = tokenConPermisos("CREAR_COMENTARIO_CONFIDENCIAL_ESTUDIANTE");

        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(ID_ESTUDIANTE);
        estudiante.setEstado(EstadoLogico.ACTIVO);
        when(estudianteRepository.findById(ID_ESTUDIANTE)).thenReturn(Optional.of(estudiante));
        when(comentarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post(BASE_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyCrear("Observación de prueba"))))
                .andExpect(status().isCreated());
    }

    // Complementa el hallazgo #12 de la auditoría (falta @Size en contenido):
    // este test confirma que al menos @NotBlank sí se ejerce de verdad en el
    // borde HTTP — algo que ningún test de service puede verificar, porque ahí
    // el DTO ya se construye válido a mano, sin pasar por la deserialización +
    // Bean Validation real de Spring MVC.
    @Test
    @DisplayName("Crear - Contenido en blanco responde 400 (Bean Validation real)")
    void crear_ContenidoEnBlanco_Responde400() throws Exception {
        String token = tokenConPermisos("CREAR_COMENTARIO_CONFIDENCIAL_ESTUDIANTE");

        mockMvc.perform(post(BASE_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyCrear("   "))))
                .andExpect(status().isBadRequest());
    }
}
