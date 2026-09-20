package com.fides.sienep.exception;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// /error lo arma Boot cuando el error no pasó por GlobalExceptionHandler (sendError de un
// filtro, o un request que rechaza el firewall). Se simulan los atributos del ERROR dispatch.
class ApiErrorAttributesTest {

    private final ApiErrorAttributes errorAttributes = new ApiErrorAttributes();

    // includeMessage=false replica producción (server.error.include-message=never, donde Boot
    // vacía el mensaje); true replica local con DevTools. El resultado tiene que ser el mismo.
    private Map<String, Object> atributos(int status, String mensaje, boolean includeMessage) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/auth/me");
        if (mensaje != null) {
            request.setAttribute(RequestDispatcher.ERROR_MESSAGE, mensaje);
        }
        ErrorAttributeOptions options = includeMessage
                ? ErrorAttributeOptions.defaults().including(ErrorAttributeOptions.Include.MESSAGE)
                : ErrorAttributeOptions.defaults();
        return errorAttributes.getErrorAttributes(new ServletWebRequest(request), options);
    }

    @Test
    @DisplayName("401 con texto propio del proyecto - Se conserva, también sin include-message (producción)")
    void status401_ConservaMensajePropio_EnProduccionYEnLocal() {
        assertEquals("Token inválido o vencido", atributos(401, "Token inválido o vencido", false).get("message"));
        assertEquals("Token inválido o vencido", atributos(401, "Token inválido o vencido", true).get("message"));
    }

    @Test
    @DisplayName("403 con texto propio (contraseña temporal) - Se conserva")
    void status403_ConservaMensajePropio() {
        String propio = "Debe cambiar su contraseña temporal antes de continuar.";
        assertEquals(propio, atributos(403, propio, false).get("message"));
    }

    @Test
    @DisplayName("401/403 con la frase estándar en inglés que agrega Spring - Se reemplaza por texto en castellano")
    void status401y403_FraseEstandarEnIngles_SeReemplaza() {
        assertEquals("No autenticado", atributos(401, "Unauthorized", true).get("message"));
        assertEquals("No autenticado", atributos(401, null, false).get("message"));
        assertEquals("No tienes permisos suficientes para realizar esta acción.",
                atributos(403, "Access Denied", true).get("message"));
        assertEquals("No tienes permisos suficientes para realizar esta acción.",
                atributos(403, "Forbidden", true).get("message"));
    }

    @Test
    @DisplayName("400 del firewall (\"No message available\") - Mensaje en castellano, nunca el de relleno de Boot")
    void status400_NoMessageAvailable_MensajeEnCastellano() {
        Map<String, Object> resultado = atributos(400, null, true);

        assertEquals("La solicitud no es válida.", resultado.get("message"));
        assertFalse(resultado.get("message").toString().contains("No message available"));
    }

    @Test
    @DisplayName("Otros 4xx - El texto sale por código, aunque la petición traiga uno propio (puede ser técnico o en inglés)")
    void otros4xx_TextoPorStatus() {
        assertEquals("No existe un endpoint para la ruta solicitada.", atributos(404, "Not Found", true).get("message"));
        assertEquals("Método no permitido para esta ruta.", atributos(405, "cualquier cosa", true).get("message"));
        assertEquals("La solicitud supera el tamaño máximo permitido.", atributos(413, null, false).get("message"));
        assertEquals("Demasiadas solicitudes. Intentá nuevamente más tarde.", atributos(429, null, false).get("message"));
        assertEquals("La solicitud no pudo procesarse.", atributos(418, null, false).get("message"));
    }

    @Test
    @DisplayName("5xx - Siempre el mensaje genérico: no filtra el texto de una excepción interna")
    void status5xx_MensajeGenerico_NoFiltraExcepcionInterna() {
        String interno = "could not execute statement; SQL [insert into proyecto.usuarios ...]";

        assertEquals(ApiErrorAttributes.MENSAJE_500, atributos(500, interno, true).get("message"));
        assertEquals(ApiErrorAttributes.MENSAJE_500, atributos(503, null, false).get("message"));
    }

    @Test
    @DisplayName("code - Si el request trae un código estable se copia al body; si no, no aparece")
    void code_SoloSiElRequestLoTrae() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Debe cambiar su contraseña temporal antes de continuar.");
        request.setAttribute(ApiErrorAttributes.ATRIBUTO_CODIGO, "PASSWORD_CHANGE_REQUIRED");

        Map<String, Object> conCodigo = errorAttributes.getErrorAttributes(
                new ServletWebRequest(request), ErrorAttributeOptions.defaults());

        assertEquals("PASSWORD_CHANGE_REQUIRED", conCodigo.get("code"));
        assertEquals("Debe cambiar su contraseña temporal antes de continuar.", conCodigo.get("message"));
        assertFalse(atributos(403, "No tienes permisos", true).containsKey("code"));
    }

    @Test
    @DisplayName("Forma del cuerpo - timestamp como el del resto de la API, y sin exception/trace/errors")
    void forma_MismoFormatoQueElResto_YSinDatosInternos() {
        Map<String, Object> resultado = atributos(401, "No autenticado", true);

        assertInstanceOf(LocalDateTime.class, resultado.get("timestamp"));
        assertEquals(401, resultado.get("status"));
        assertEquals("Unauthorized", resultado.get("error"));
        assertEquals("/auth/me", resultado.get("path"));
        assertFalse(resultado.containsKey("exception"));
        assertFalse(resultado.containsKey("trace"));
        assertFalse(resultado.containsKey("errors"));
    }
}
