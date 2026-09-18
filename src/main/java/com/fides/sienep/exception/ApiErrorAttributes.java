package com.fides.sienep.exception;

import jakarta.servlet.RequestDispatcher;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cuerpo JSON de /error: lo que responde Spring Boot cuando el error NO pasó por
 * {@link GlobalExceptionHandler} — un {@code response.sendError(...)} de un filtro
 * (JwtRequestFilter, el entry point de 401), o un request que el firewall de Spring
 * Security rechaza antes de llegar a ningún controlador (ej. "//carreras", ";" en la ruta).
 *
 * Por qué existe: el frontend muestra el campo {@code message} tal cual, y el
 * formato por defecto de Boot no sirve para eso:
 *  - Fuera de desarrollo NO trae {@code message} (server.error.include-message=never).
 *    Con DevTools sí lo trae, así que en local parecía andar y en producción no.
 *  - Cuando lo trae y no hay motivo, dice "No message available" (inglés, inútil).
 *  - Con include-message activado podría filtrar el mensaje de una excepción interna.
 *
 * Acá el mensaje se decide por código HTTP y siempre sale en castellano, sin
 * depender de esas propiedades. Solo se conservan los textos propios de los 401/403
 * que arma este proyecto ("No autenticado", "Token inválido o vencido", ...), y los
 * 5xx salen siempre con el mensaje genérico. Se descartan además "exception", "trace"
 * y "errors", que DevTools puede activar.
 *
 * Mismo formato de timestamp que GlobalExceptionHandler (LocalDateTime, sin zona).
 */
@Component
public class ApiErrorAttributes extends DefaultErrorAttributes {

    private static final String SIN_MENSAJE_DE_BOOT = "No message available";
    static final String MENSAJE_500 = "Ocurrió un error inesperado.";

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> base = super.getErrorAttributes(webRequest, options);

        int status = (base.get("status") instanceof Integer s) ? s : HttpStatus.INTERNAL_SERVER_ERROR.value();

        // Se lee directo del request y no de base.get("message"): ese valor lo vacía
        // Boot cuando include-message=never, y los textos propios de los 401/403 se
        // perderían justo en producción.
        Object mensajeCrudo = webRequest.getAttribute(RequestDispatcher.ERROR_MESSAGE, RequestAttributes.SCOPE_REQUEST);

        Map<String, Object> atributos = new LinkedHashMap<>();
        atributos.put("timestamp", LocalDateTime.now());
        atributos.put("status", base.get("status"));
        atributos.put("error", base.get("error"));
        atributos.put("message", mensajeParaElCliente(status, mensajeCrudo));
        if (base.get("path") != null) {
            atributos.put("path", base.get("path"));
        }
        return atributos;
    }

    static String mensajeParaElCliente(int status, Object mensajeCrudo) {
        if (status >= 500) {
            return MENSAJE_500;
        }
        if ((status == 401 || status == 403) && esMensajePropio(status, mensajeCrudo)) {
            return mensajeCrudo.toString();
        }
        return mensajePorStatus(status);
    }

    // Un mensaje "propio" es el que puso este proyecto en un sendError(...). No lo es
    // ni un texto vacío, ni el de relleno de Boot, ni la frase estándar del código
    // ("Unauthorized", "Forbidden", "Access Denied") que agregan Spring y Tomcat solos.
    private static boolean esMensajePropio(int status, Object mensajeCrudo) {
        if (!(mensajeCrudo instanceof String texto) || texto.isBlank() || SIN_MENSAJE_DE_BOOT.equals(texto)) {
            return false;
        }
        HttpStatus http = HttpStatus.resolve(status);
        boolean esFraseEstandar = http != null && texto.equalsIgnoreCase(http.getReasonPhrase());
        return !esFraseEstandar && !"Access Denied".equalsIgnoreCase(texto);
    }

    private static String mensajePorStatus(int status) {
        return switch (status) {
            case 400 -> "La solicitud no es válida.";
            case 401 -> "No autenticado";
            case 403 -> "No tienes permisos suficientes para realizar esta acción.";
            case 404 -> "No existe un endpoint para la ruta solicitada.";
            case 405 -> "Método no permitido para esta ruta.";
            case 406 -> "El formato de respuesta solicitado no está soportado.";
            case 408 -> "La solicitud tardó demasiado en completarse.";
            case 413 -> "La solicitud supera el tamaño máximo permitido.";
            case 414 -> "La URL es demasiado larga.";
            case 415 -> "El tipo de contenido enviado no está soportado.";
            case 429 -> "Demasiadas solicitudes. Intentá nuevamente más tarde.";
            case 431 -> "Los encabezados de la solicitud son demasiado grandes.";
            default -> "La solicitud no pudo procesarse.";
        };
    }
}
