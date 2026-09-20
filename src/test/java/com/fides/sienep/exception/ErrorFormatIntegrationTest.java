package com.fides.sienep.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Permiso;
import com.fides.sienep.model.Rol;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.utils.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

// Recorrido real: servidor HTTP levantado (Tomcat), SecurityFilterChain de verdad y el ERROR
// dispatch a /error — algo que MockMvc no hace, por eso ApiErrorAttributesTest solo no alcanza.
// server.error.include-message=never replica producción: con DevTools (local) Boot sí agrega el
// message, y es justo esa diferencia la que esta clase existe para cubrir.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.error.include-message=never")
@ActiveProfiles("test")
class ErrorFormatIntegrationTest {

    private static final int ID_FUNCIONARIO = 3;

    @LocalServerPort private int port;
    @Autowired private JwtTokenUtil jwtTokenUtil;

    @MockitoBean private FuncionarioRepository funcionarioRepository;
    @MockitoBean private EstudianteRepository estudianteRepository;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    private JsonNode get(String path, String bearer, int statusEsperado) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(statusEsperado, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
        return json.readTree(response.body());
    }

    @Test
    @DisplayName("Sin token en una ruta protegida - 401 con message en castellano (sin include-message)")
    void sinToken_Responde401ConMessage() throws Exception {
        JsonNode body = get("/carreras", null, 401);

        assertEquals("No autenticado", body.get("message").asText());
        assertFalse(body.has("code"));
    }

    @Test
    @DisplayName("Token inválido - 401 con \"Token inválido o vencido\"")
    void tokenInvalido_Responde401ConMessage() throws Exception {
        JsonNode body = get("/carreras", "no-es-un-jwt", 401);

        assertEquals("Token inválido o vencido", body.get("message").asText());
    }

    @Test
    @DisplayName("URL rechazada por el firewall (//) - 400 con message en castellano, no \"No message available\"")
    void firewall_Responde400ConMessageEnCastellano() throws Exception {
        JsonNode body = get("//carreras", null, 400);

        assertEquals("La solicitud no es válida.", body.get("message").asText());
    }

    @Test
    @DisplayName("Usuario con contraseña temporal fuera de la whitelist - 403 con message y code PASSWORD_CHANGE_REQUIRED")
    void usuarioPendiente_Responde403ConCode() throws Exception {
        Rol rol = new Rol("ADMINISTRADOR");
        rol.agregarPermiso(new Permiso("VER_CARRERAS", "descripcion de prueba"));
        Funcionario funcionario = new Funcionario();
        funcionario.setIdUsuario(ID_FUNCIONARIO);
        funcionario.setEmail("pendiente@utec.edu.uy");
        funcionario.setEstado(EstadoLogico.PENDIENTE_DE_ACTIVACION);
        funcionario.setRol(rol);
        when(funcionarioRepository.findById(ID_FUNCIONARIO)).thenReturn(Optional.of(funcionario));
        String token = jwtTokenUtil.generateToken(ID_FUNCIONARIO, rol.getNombre());

        JsonNode body = get("/carreras", token, 403);

        assertEquals("Debe cambiar su contraseña temporal antes de continuar.", body.get("message").asText());
        assertEquals("PASSWORD_CHANGE_REQUIRED", body.get("code").asText());
    }
}
