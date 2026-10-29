package com.fides.sienep.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class EmailServiceTest {

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private MockRestServiceServer mockServer;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BREVO_URL)
                .defaultHeader("api-key", "test-api-key")
                .defaultHeader("Content-Type", "application/json");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        emailService = new EmailService(builder.build());
        // @Value no se procesa al instanciar "a mano" fuera de Spring — mismo patrón
        // que AuthServiceTest para su propio frontendUrl.
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");
    }

    @Test
    @DisplayName("enviarContraseniaTemporal manda el JSON esperado a la API de Brevo")
    void enviarContraseniaTemporal_ok() {
        mockServer.expect(requestTo(BREVO_URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("api-key", "test-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "sender": {"name": "Notificaciones SIENEP", "email": "notificaciones@sienep.app"},
                          "to": [{"email": "juan@utec.edu.uy"}],
                          "subject": "Acceso al sistema - Contraseña temporal"
                        }
                        """))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertDoesNotThrow(() ->
                emailService.enviarContraseniaTemporal("juan@utec.edu.uy", "Juan", "temp1234"));
        mockServer.verify();
    }

    @Test
    @DisplayName("enviarContraseniaTemporal - el HTML lleva los valores interpolados, sin placeholders sin resolver")
    void enviarContraseniaTemporal_HtmlConValoresInterpolados() {
        mockServer.expect(requestTo(BREVO_URL))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("\"htmlContent\""),
                        org.hamcrest.Matchers.containsString("Juan"),
                        org.hamcrest.Matchers.containsString("temp1234"),
                        org.hamcrest.Matchers.containsString("http://localhost:3000"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("{{")))))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertDoesNotThrow(() ->
                emailService.enviarContraseniaTemporal("juan@utec.edu.uy", "Juan", "temp1234"));
        mockServer.verify();
    }

    @Test
    @DisplayName("enviarContraseniaTemporal lanza 500 si Brevo falla")
    void enviarContraseniaTemporal_falloBrevo_lanza500() {
        mockServer.expect(requestTo(BREVO_URL)).andRespond(withServerError());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                emailService.enviarContraseniaTemporal("juan@utec.edu.uy", "Juan", "temp1234"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    }

    @Test
    @DisplayName("enviarContraseniaRestablecida lanza 503 (no 500) si Brevo falla")
    void enviarContraseniaRestablecida_falloBrevo_lanza503() {
        mockServer.expect(requestTo(BREVO_URL)).andRespond(withServerError());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                emailService.enviarContraseniaRestablecida("juan@utec.edu.uy", "Juan", "temp1234"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    @DisplayName("enviarContraseniaRestablecida - el HTML lleva los valores interpolados, sin placeholders sin resolver")
    void enviarContraseniaRestablecida_HtmlConValoresInterpolados() {
        mockServer.expect(requestTo(BREVO_URL))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("\"htmlContent\""),
                        org.hamcrest.Matchers.containsString("Juan"),
                        org.hamcrest.Matchers.containsString("temp1234"),
                        org.hamcrest.Matchers.containsString("http://localhost:3000"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("{{")))))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertDoesNotThrow(() ->
                emailService.enviarContraseniaRestablecida("juan@utec.edu.uy", "Juan", "temp1234"));
        mockServer.verify();
    }

    @Test
    @DisplayName("enviarLinkRestablecimiento manda el link en el cuerpo del mail")
    void enviarLinkRestablecimiento_ok() {
        mockServer.expect(requestTo(BREVO_URL))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "https://sienep.app/restablecer?token=abc123")))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertDoesNotThrow(() -> emailService.enviarLinkRestablecimiento(
                "juan@utec.edu.uy", "Juan", "https://sienep.app/restablecer?token=abc123"));
        mockServer.verify();
    }

    @Test
    @DisplayName("enviarLinkRestablecimiento - el HTML lleva los valores interpolados, sin placeholders sin resolver")
    void enviarLinkRestablecimiento_HtmlConValoresInterpolados() {
        mockServer.expect(requestTo(BREVO_URL))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("\"htmlContent\""),
                        org.hamcrest.Matchers.containsString("Juan"),
                        org.hamcrest.Matchers.containsString("https://sienep.app/restablecer?token=abc123"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("{{")))))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertDoesNotThrow(() -> emailService.enviarLinkRestablecimiento(
                "juan@utec.edu.uy", "Juan", "https://sienep.app/restablecer?token=abc123"));
        mockServer.verify();
    }

    @Test
    @DisplayName("enviarNotificacionInstancia NO relanza la excepción si Brevo falla y devuelve false")
    void enviarNotificacionInstancia_falloBrevo_noLanza() {
        mockServer.expect(requestTo(BREVO_URL)).andRespond(withServerError());

        boolean enviada = assertDoesNotThrow(() -> emailService.enviarNotificacionInstancia(
                "juan@utec.edu.uy", "Juan", "INST-2026-000001", "Seguimiento", LocalDateTime.now().plusDays(1)));
        assertFalse(enviada);
    }

    @Test
    @DisplayName("enviarNotificacionInstancia - el HTML lleva los valores interpolados, sin placeholders sin resolver")
    void enviarNotificacionInstancia_HtmlConValoresInterpolados() {
        mockServer.expect(requestTo(BREVO_URL))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("\"htmlContent\""),
                        org.hamcrest.Matchers.containsString("Juan"),
                        org.hamcrest.Matchers.containsString("INST-2026-000001"),
                        org.hamcrest.Matchers.containsString("Seguimiento"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("{{")))))
                .andRespond(withStatus(HttpStatus.CREATED));

        boolean enviada = assertDoesNotThrow(() -> emailService.enviarNotificacionInstancia(
                "juan@utec.edu.uy", "Juan", "INST-2026-000001", "Seguimiento", LocalDateTime.now().plusDays(1)));
        assertTrue(enviada);
        mockServer.verify();
    }

    @Test
    @DisplayName("Plantilla - Un valor que contiene {{OTRA_CLAVE}} no es pisado por un reemplazo posterior")
    void plantilla_ValorConPlaceholder_NoSePisa() {
        // Un título de instancia lo escribe un usuario: si trae "{{NOMBRE_USUARIO}}", tiene que salir
        // literal. Aparece 2 veces en el body: en el texto plano y en el HTML. Con el reemplazo
        // clave por clave, según el orden del Map, el del HTML se convertía en el nombre ("Juan").
        mockServer.expect(requestTo(BREVO_URL))
                .andExpect(request -> {
                    String body = ((org.springframework.mock.http.client.MockClientHttpRequest) request).getBodyAsString();
                    int ocurrencias = body.split(java.util.regex.Pattern.quote("{{NOMBRE_USUARIO}}"), -1).length - 1;
                    assertEquals(2, ocurrencias);
                })
                .andRespond(withStatus(HttpStatus.CREATED));

        assertDoesNotThrow(() -> emailService.enviarNotificacionInstancia(
                "juan@utec.edu.uy", "Juan", "INST-2026-000001", "{{NOMBRE_USUARIO}}", LocalDateTime.now()));
        mockServer.verify();
    }

    @Test
    @DisplayName("Plantilla - Un placeholder sin valor falla en vez de dejar {{CLAVE}} literal en el mail")
    void plantilla_PlaceholderSinValor_Falla() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                emailService.renderizarPlantilla("email-templates/prueba-dos-placeholders.html",
                        java.util.Map.of("NOMBRE", "Juan")));

        org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains("{{CODIGO}}"), e.getMessage());
    }

    @Test
    @DisplayName("Plantilla - Con todos los valores se reemplaza y se escapa el HTML")
    void plantilla_TodosLosValores_Renderiza() {
        String html = emailService.renderizarPlantilla("email-templates/prueba-dos-placeholders.html",
                java.util.Map.of("NOMBRE", "<Juan>", "CODIGO", "123"));

        assertEquals("<p>Hola &lt;Juan&gt;, tu código es 123.</p>\n", html);
    }

    @Test
    @DisplayName("enviarNotificacionRecordatorio NO relanza la excepción si Brevo falla (solo warn)")
    void enviarNotificacionRecordatorio_falloBrevo_noLanza() {
        mockServer.expect(requestTo(BREVO_URL)).andRespond(withServerError());

        assertDoesNotThrow(() -> emailService.enviarNotificacionRecordatorio(
                "juan@utec.edu.uy", "Juan", "REC-2026-000001", "Control", LocalDateTime.now()));
    }

    @Test
    @DisplayName("enviarNotificacionRecordatorio - el HTML lleva los valores interpolados, sin placeholders sin resolver")
    void enviarNotificacionRecordatorio_HtmlConValoresInterpolados() {
        mockServer.expect(requestTo(BREVO_URL))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("\"htmlContent\""),
                        org.hamcrest.Matchers.containsString("Juan"),
                        org.hamcrest.Matchers.containsString("REC-2026-000001"),
                        org.hamcrest.Matchers.containsString("Control"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("{{")))))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertDoesNotThrow(() -> emailService.enviarNotificacionRecordatorio(
                "juan@utec.edu.uy", "Juan", "REC-2026-000001", "Control", LocalDateTime.now()));
        mockServer.verify();
    }

    @Test
    @DisplayName("enviarRecordatorioProximo NO relanza la excepción si Brevo falla y devuelve false")
    void enviarRecordatorioProximo_falloBrevo_noLanza() {
        mockServer.expect(requestTo(BREVO_URL)).andRespond(withServerError());

        boolean enviada = assertDoesNotThrow(() -> emailService.enviarRecordatorioProximo(
                "juan@utec.edu.uy", "Juan", "REC-2026-000001", "Control", LocalDateTime.now()));
        assertFalse(enviada);
    }

    @Test
    @DisplayName("enviarRecordatorioProximo - el HTML lleva los valores interpolados, sin placeholders sin resolver")
    void enviarRecordatorioProximo_HtmlConValoresInterpolados() {
        mockServer.expect(requestTo(BREVO_URL))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("\"htmlContent\""),
                        org.hamcrest.Matchers.containsString("Juan"),
                        org.hamcrest.Matchers.containsString("REC-2026-000001"),
                        org.hamcrest.Matchers.containsString("Control"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("{{")))))
                .andRespond(withStatus(HttpStatus.CREATED));

        boolean enviada = assertDoesNotThrow(() -> emailService.enviarRecordatorioProximo(
                "juan@utec.edu.uy", "Juan", "REC-2026-000001", "Control", LocalDateTime.now()));
        assertTrue(enviada);
        mockServer.verify();
    }
}
