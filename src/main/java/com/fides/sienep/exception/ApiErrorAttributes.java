package com.fides.sienep.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cuerpo JSON de /error: lo que responde Spring Boot cuando el error NO pasó por
 * {@link GlobalExceptionHandler} — un error que un filtro responde con sendError(...)
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
 * Cómo decide el mensaje: el que responde un error del proyecto lo declara con
 * {@link #enviarError}, que lo guarda en un atributo del request; SOLO ese texto pasa al
 * cliente. Cualquier otro (el de Tomcat, el firewall, una librería que llame a
 * sendError con su propio texto, seguramente en inglés) se ignora y se reemplaza por uno
 * en castellano según el código HTTP. Los 5xx salen siempre con el mensaje genérico.
 * Se descartan además "exception", "trace" y "errors", que DevTools puede activar.
 *
 * Formato: mismo timestamp que GlobalExceptionHandler (LocalDateTime, sin zona). El campo
 * "error" es la frase HTTP estándar en inglés que arma Boot ("Unauthorized", "Bad Request"):
 * es una etiqueta corta para el código, NO un texto para mostrar. El texto para el usuario
 * es siempre "message". (GlobalExceptionHandler también emite "error" con etiquetas en
 * castellano en varios casos; el cliente no debe depender de su valor.)
 */
@Component
public class ApiErrorAttributes extends DefaultErrorAttributes {

    static final String MENSAJE_500 = "Ocurrió un error inesperado.";

    // Mensaje que el proyecto quiere mostrar al cliente (ej. "Token inválido o vencido").
    public static final String ATRIBUTO_MENSAJE = "sienep.error.mensaje";

    // Código de error estable (ej. "PASSWORD_CHANGE_REQUIRED"), se copia al body como "code":
    // el frontend distingue así un caso puntual sin comparar el texto del message.
    public static final String ATRIBUTO_CODIGO = "sienep.error.codigo";

    /**
     * Responde un error desde un filtro o entry point. Es la única forma de que un texto
     * propio llegue al body de /error: se declara acá, no se deduce del texto del sendError.
     *
     * @param codigo código estable para el frontend, o null si no hace falta
     */
    public static void enviarError(HttpServletRequest request, HttpServletResponse response,
                                   int status, String mensaje, String codigo) throws IOException {
        request.setAttribute(ATRIBUTO_MENSAJE, mensaje);
        if (codigo != null) {
            request.setAttribute(ATRIBUTO_CODIGO, codigo);
        }
        // El texto del sendError solo sirve para el log y la página de error del contenedor:
        // el body de /error usa el atributo.
        response.sendError(status, mensaje);
    }

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> base = super.getErrorAttributes(webRequest, options);

        int status = (base.get("status") instanceof Integer s) ? s : HttpStatus.INTERNAL_SERVER_ERROR.value();
        Object mensajePropio = webRequest.getAttribute(ATRIBUTO_MENSAJE, RequestAttributes.SCOPE_REQUEST);

        Map<String, Object> atributos = new LinkedHashMap<>();
        atributos.put("timestamp", LocalDateTime.now());
        atributos.put("status", base.get("status"));
        atributos.put("error", base.get("error"));
        atributos.put("message", mensajeParaElCliente(status, mensajePropio));
        if (base.get("path") != null) {
            atributos.put("path", base.get("path"));
        }
        Object codigo = webRequest.getAttribute(ATRIBUTO_CODIGO, RequestAttributes.SCOPE_REQUEST);
        if (codigo instanceof String texto && !texto.isBlank()) {
            atributos.put("code", texto);
        }
        return atributos;
    }

    static String mensajeParaElCliente(int status, Object mensajePropio) {
        if (status >= 500) {
            return MENSAJE_500;
        }
        if (mensajePropio instanceof String texto && !texto.isBlank()) {
            return texto;
        }
        return mensajePorStatus(status);
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
            case 429 -> "Demasiadas solicitudes. Intenta nuevamente más tarde.";
            case 431 -> "Los encabezados de la solicitud son demasiado grandes.";
            default -> "La solicitud no pudo procesarse.";
        };
    }
}
