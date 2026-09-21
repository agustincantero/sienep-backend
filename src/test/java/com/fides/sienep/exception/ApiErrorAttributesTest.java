package com.fides.sienep.exception;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// /error lo arma Boot cuando el error no pasó por GlobalExceptionHandler (sendError de un
// filtro, o un request que rechaza el firewall). Se simulan los atributos del ERROR dispatch.
// El recorrido completo con el pipeline real está en ErrorFormatIntegrationTest.
class ApiErrorAttributesTest {

    private final ApiErrorAttributes errorAttributes = new ApiErrorAttributes();

    // mensajeCrudo: lo que puso el contenedor/una librería en el sendError (jakarta.servlet.error.message).
    // mensajePropio: lo que declaró el proyecto con enviarError(...). includeMessage=false replica
    // producción (server.error.include-message=never); true replica local con DevTools.
    private Map<String, Object> atributos(int status, String mensajeCrudo, String mensajePropio, boolean includeMessage) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/auth/me");
        if (mensajeCrudo != null) {
            request.setAttribute(RequestDispatcher.ERROR_MESSAGE, mensajeCrudo);
        }
        if (mensajePropio != null) {
            request.setAttribute(ApiErrorAttributes.ATRIBUTO_MENSAJE, mensajePropio);
        }
        ErrorAttributeOptions options = includeMessage
                ? ErrorAttributeOptions.defaults().including(ErrorAttributeOptions.Include.MESSAGE)
                : ErrorAttributeOptions.defaults();
        return errorAttributes.getErrorAttributes(new ServletWebRequest(request), options);
    }

    @Test
    @DisplayName("Mensaje declarado por el proyecto - Pasa al cliente, también sin include-message (producción)")
    void mensajeDeclarado_Pasa_EnProduccionYEnLocal() {
        assertEquals("Token inválido o vencido",
                atributos(401, "Token inválido o vencido", "Token inválido o vencido", false).get("message"));
        assertEquals("Token inválido o vencido",
                atributos(401, "Token inválido o vencido", "Token inválido o vencido", true).get("message"));
    }

    @Test
    @DisplayName("Texto de una librería en un 401/403 sin declarar (ej. en inglés) - NO pasa, sale el genérico en castellano")
    void textoSinDeclarar_NoPasa() {
        assertEquals("No autenticado", atributos(401, "Full authentication is required", null, true).get("message"));
        assertEquals("No autenticado", atributos(401, "Unauthorized", null, true).get("message"));
        assertEquals("No tienes permisos suficientes para realizar esta acción.",
                atributos(403, "Access Denied", null, true).get("message"));
        assertEquals("No tienes permisos suficientes para realizar esta acción.",
                atributos(403, "Forbidden", null, false).get("message"));
    }

    @Test
    @DisplayName("Mensaje declarado en un 403 (contraseña temporal) - Se conserva")
    void status403_ConservaMensajeDeclarado() {
        String propio = "Debe cambiar su contraseña temporal antes de continuar.";
        assertEquals(propio, atributos(403, propio, propio, false).get("message"));
    }

    @Test
    @DisplayName("400 del firewall (\"No message available\") - Mensaje en castellano, nunca el de relleno de Boot")
    void status400_NoMessageAvailable_MensajeEnCastellano() {
        Map<String, Object> resultado = atributos(400, "No message available", null, true);

        assertEquals("La solicitud no es válida.", resultado.get("message"));
        assertFalse(resultado.get("message").toString().contains("No message available"));
    }

    @Test
    @DisplayName("Otros 4xx sin mensaje declarado - El texto sale por código, aunque el contenedor traiga uno")
    void otros4xx_TextoPorStatus() {
        assertEquals("No existe un endpoint para la ruta solicitada.", atributos(404, "Not Found", null, true).get("message"));
        assertEquals("Método no permitido para esta ruta.", atributos(405, "cualquier cosa", null, true).get("message"));
        assertEquals("La solicitud supera el tamaño máximo permitido.", atributos(413, null, null, false).get("message"));
        assertEquals("Demasiadas solicitudes. Intenta nuevamente más tarde.", atributos(429, null, null, false).get("message"));
        assertEquals("La solicitud no pudo procesarse.", atributos(418, null, null, false).get("message"));
    }

    @Test
    @DisplayName("5xx - Siempre el mensaje genérico: no filtra el texto de una excepción interna, ni uno declarado")
    void status5xx_MensajeGenerico_NoFiltraExcepcionInterna() {
        String interno = "could not execute statement; SQL [insert into proyecto.usuarios ...]";

        assertEquals(ApiErrorAttributes.MENSAJE_500, atributos(500, interno, null, true).get("message"));
        assertEquals(ApiErrorAttributes.MENSAJE_500, atributos(500, interno, interno, true).get("message"));
        assertEquals(ApiErrorAttributes.MENSAJE_500, atributos(503, null, null, false).get("message"));
    }

    @Test
    @DisplayName("enviarError - Declara mensaje y code en el request y responde el error con ese status")
    void enviarError_DeclaraMensajeYCodigo() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ApiErrorAttributes.enviarError(request, response, 403, "Debe cambiar su contraseña", "PASSWORD_CHANGE_REQUIRED");

        assertEquals("Debe cambiar su contraseña", request.getAttribute(ApiErrorAttributes.ATRIBUTO_MENSAJE));
        assertEquals("PASSWORD_CHANGE_REQUIRED", request.getAttribute(ApiErrorAttributes.ATRIBUTO_CODIGO));
        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("code - Si el request trae un código estable se copia al body; si no, no aparece")
    void code_SoloSiElRequestLoTrae() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
        request.setAttribute(ApiErrorAttributes.ATRIBUTO_MENSAJE, "Debe cambiar su contraseña temporal antes de continuar.");
        request.setAttribute(ApiErrorAttributes.ATRIBUTO_CODIGO, "PASSWORD_CHANGE_REQUIRED");

        Map<String, Object> conCodigo = errorAttributes.getErrorAttributes(
                new ServletWebRequest(request), ErrorAttributeOptions.defaults());

        assertEquals("PASSWORD_CHANGE_REQUIRED", conCodigo.get("code"));
        assertEquals("Debe cambiar su contraseña temporal antes de continuar.", conCodigo.get("message"));
        assertFalse(atributos(403, null, "No tienes permisos", true).containsKey("code"));
    }

    @Test
    @DisplayName("Forma del cuerpo - timestamp como el del resto de la API, y sin exception/trace/errors")
    void forma_MismoFormatoQueElResto_YSinDatosInternos() {
        Map<String, Object> resultado = atributos(401, null, "No autenticado", true);

        assertInstanceOf(LocalDateTime.class, resultado.get("timestamp"));
        assertEquals(401, resultado.get("status"));
        assertEquals("Unauthorized", resultado.get("error"));
        assertEquals("/auth/me", resultado.get("path"));
        assertFalse(resultado.containsKey("exception"));
        assertFalse(resultado.containsKey("trace"));
        assertFalse(resultado.containsKey("errors"));
    }
}
