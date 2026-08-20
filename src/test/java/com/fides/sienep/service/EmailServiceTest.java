package com.fides.sienep.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                          "sender": {"email": "no-reply@sienep.app"},
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
    @DisplayName("enviarNotificacionInstancia NO relanza la excepción si Brevo falla (solo warn)")
    void enviarNotificacionInstancia_falloBrevo_noLanza() {
        mockServer.expect(requestTo(BREVO_URL)).andRespond(withServerError());

        assertDoesNotThrow(() -> emailService.enviarNotificacionInstancia(
                "juan@utec.edu.uy", "Juan", "INST-2026-000001", "Seguimiento"));
    }

    @Test
    @DisplayName("enviarNotificacionRecordatorio NO relanza la excepción si Brevo falla (solo warn)")
    void enviarNotificacionRecordatorio_falloBrevo_noLanza() {
        mockServer.expect(requestTo(BREVO_URL)).andRespond(withServerError());

        assertDoesNotThrow(() -> emailService.enviarNotificacionRecordatorio(
                "juan@utec.edu.uy", "Juan", "REC-2026-000001", "Control", LocalDateTime.now()));
    }

    @Test
    @DisplayName("enviarRecordatorioProximo NO relanza la excepción si Brevo falla (solo warn)")
    void enviarRecordatorioProximo_falloBrevo_noLanza() {
        mockServer.expect(requestTo(BREVO_URL)).andRespond(withServerError());

        assertDoesNotThrow(() -> emailService.enviarRecordatorioProximo(
                "juan@utec.edu.uy", "Juan", "REC-2026-000001", "Control", LocalDateTime.now()));
    }
}
