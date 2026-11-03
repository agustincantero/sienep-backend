package com.fides.sienep.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ── Validación de DTOs (MethodArgumentNotValidException) ─────────────────

    private MethodArgumentNotValidException validationException(org.springframework.validation.ObjectError... errores) {
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "dto");
        for (var error : errores) {
            resultado.addError(error);
        }
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(resultado);
        return ex;
    }

    @Test
    @DisplayName("Validación de DTO - Responde 400 con message (el frontend lo muestra tal cual) y detalles por campo")
    @SuppressWarnings("unchecked")
    void handleValidation_IncluyeMessageYDetalles() {
        var ex = validationException(
                new FieldError("dto", "nomGrupo", "", false, null, null, "no debe estar vacío"),
                new FieldError("dto", "generacion", null, false, null, null, "no debe ser nulo"));

        ResponseEntity<Map<String, Object>> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(GlobalExceptionHandler.MENSAJE_VALIDACION, response.getBody().get("message"));
        Map<String, String> detalles = (Map<String, String>) response.getBody().get("detalles");
        assertEquals("no debe estar vacío", detalles.get("nomGrupo"));
        assertEquals("no debe ser nulo", detalles.get("generacion"));
    }

    @Test
    @DisplayName("Validación de DTO - Un valor de tipo inválido no expone el texto técnico de Spring (clases Java, inglés)")
    @SuppressWarnings("unchecked")
    void handleValidation_ErrorDeConversionDeTipo_MensajeLegible() {
        var ex = validationException(new FieldError("dto", "idUsuario", "abc", true, null, null,
                "Failed to convert property value of type 'java.lang.String' to required type "
                        + "'java.lang.Integer' for property 'idUsuario'; For input string: \"abc\""));

        Map<String, String> detalles =
                (Map<String, String>) handler.handleValidationExceptions(ex).getBody().get("detalles");

        assertEquals(GlobalExceptionHandler.MENSAJE_FORMATO_INVALIDO, detalles.get("idUsuario"));
    }

    @Test
    @DisplayName("Validación de DTO - Un error a nivel de objeto (no FieldError) no rompe el handler con ClassCastException")
    @SuppressWarnings("unchecked")
    void handleValidation_ErrorDeObjeto_NoRompe() {
        var ex = validationException(new org.springframework.validation.ObjectError("dto", "Las fechas no son coherentes"));

        ResponseEntity<Map<String, Object>> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> detalles = (Map<String, String>) response.getBody().get("detalles");
        assertEquals("Las fechas no son coherentes", detalles.get("dto"));
    }

    @Test
    @DisplayName("Validación de DTO - Si un campo falla más de una constraint, muestra uno solo (el primero que llega, no el último)")
    @SuppressWarnings("unchecked")
    void handleValidation_VariosErroresEnUnCampo_PrimeroGana() {
        var ex = validationException(
                new FieldError("dto", "nombre", "", false, null, null, "primero"),
                new FieldError("dto", "nombre", "", false, null, null, "segundo"));

        Map<String, String> detalles =
                (Map<String, String>) handler.handleValidationExceptions(ex).getBody().get("detalles");

        assertEquals("primero", detalles.get("nombre"));
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException - Responde 400 (H12)")
    void handleMethodArgumentTypeMismatch_Responde400() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");

        ResponseEntity<Map<String, Object>> response = handler.handleMethodArgumentTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().get("status"));
    }

    @Test
    @DisplayName("DataIntegrityViolationException - Responde 409 (H12)")
    void handleDataIntegrityViolation_Responde409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("duplicate key value violates unique constraint");

        ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().get("status"));
        assertEquals("La operación no pudo completarse por un conflicto con datos existentes.",
                response.getBody().get("message"));
    }

    @Test
    @DisplayName("DataIntegrityViolationException por uk_documento_pais - El mensaje nombra el documento")
    void handleDataIntegrityViolation_UkDocumentoPais_MensajeDeDocumento() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("ERROR: duplicate key value violates unique constraint \"uk_documento_pais\""));

        ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Ya hay un usuario registrado con ese documento.", response.getBody().get("message"));
    }

    @Test
    @DisplayName("DataIntegrityViolationException por uk_email - El mensaje nombra el email")
    void handleDataIntegrityViolation_UkEmail_MensajeDeEmail() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("ERROR: duplicate key value violates unique constraint \"uk_email\""));

        ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Ya existe un usuario con ese email.", response.getBody().get("message"));
    }

    @Test
    @DisplayName("NoResourceFoundException - Responde 404 en vez de 500")
    void handleNoResourceFound_Responde404() {
        ResponseEntity<Map<String, Object>> response = handler.handleNoResourceFound();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().get("status"));
    }

    @Test
    @DisplayName("MissingServletRequestParameterException - Responde 400 en vez de 500")
    void handleMissingServletRequestParameter_Responde400() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("idUsuario", "Integer");

        ResponseEntity<Map<String, Object>> response = handler.handleMissingServletRequestParameter(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().get("status"));
    }

    @Test
    @DisplayName("MissingServletRequestPartException - Responde 400 en vez de 500")
    void handleMissingServletRequestPart_Responde400() {
        MissingServletRequestPartException ex = new MissingServletRequestPartException("archivo");

        ResponseEntity<Map<String, Object>> response = handler.handleMissingServletRequestPart(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().get("status"));
    }

    @Test
    @DisplayName("HttpMediaTypeNotSupportedException - Responde 415 en vez de 500")
    void handleHttpMediaTypeNotSupported_Responde415() {
        ResponseEntity<Map<String, Object>> response = handler.handleHttpMediaTypeNotSupported();

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), response.getBody().get("status"));
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException - Responde 405 en vez de 500")
    void handleHttpRequestMethodNotSupported_Responde405() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("PATCH");

        ResponseEntity<Map<String, Object>> response = handler.handleHttpRequestMethodNotSupported(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), response.getBody().get("status"));
    }

    @Test
    @DisplayName("PropertyReferenceException - Responde 400 en vez de 500 (ej. ?sort= con una propiedad inexistente)")
    void handlePropertyReference_Responde400() {
        PropertyReferenceException ex = mock(PropertyReferenceException.class);
        when(ex.getPropertyName()).thenReturn("campoQueNoExiste");

        ResponseEntity<Map<String, Object>> response = handler.handlePropertyReference(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().get("status"));
    }

    @Test
    @DisplayName("InvalidDataAccessApiUsageException - Responde 400 en vez de 500 (ej. ?page= tan grande que desborda el offset)")
    void handleInvalidDataAccessApiUsage_Responde400() {
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidDataAccessApiUsage();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().get("status"));
    }

    @Test
    @DisplayName("MultipartException - Responde 400 en vez de 500 (ej. nombre de archivo con byte nulo)")
    void handleMultipartException_Responde400() {
        ResponseEntity<Map<String, Object>> response = handler.handleMultipartException();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().get("status"));
    }
}
