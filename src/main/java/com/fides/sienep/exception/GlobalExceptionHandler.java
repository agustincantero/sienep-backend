package com.fides.sienep.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor global de excepciones para toda la API.
 * Se encarga de capturar excepciones lanzadas por los controladores o servicios
 * y transformarlas en respuestas JSON estandarizadas y limpias para el Frontend.
 *
 * Captura:
 * - MethodArgumentNotValidException: Errores de validación de DTOs (ej: @NotBlank)
 * - HttpMessageNotReadableException: JSON mal formado o valores inválidos (ej: Enums)
 * - ResponseStatusException: Errores controlados de lógica de negocio o validaciones
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Ataja las validaciones de los DTOs (@NotBlank, @Pattern, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Error de Validación en los datos enviados");

        // Recolecta exactamente qué campos fallaron y el mensaje personalizado
        Map<String, String> detalles = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            detalles.put(fieldName, errorMessage);
        });
        body.put("detalles", detalles);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 2. Ataja errores de Jackson (JSON mal armado o Enums inválidos)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Formato de petición inválido");
        body.put("message", "El JSON enviado tiene errores de formato o contiene valores no permitidos.");

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 3. Ataja los errores manuales del Service (ej: reglas de negocio)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", ex.getStatusCode().value());
        
        // Obtiene el texto estándar del código HTTP (ej: "Not Found", "Bad Request")
        String errorName = HttpStatus.valueOf(ex.getStatusCode().value()).getReasonPhrase();
        body.put("error", errorName);
        
        body.put("message", ex.getReason());

        return new ResponseEntity<>(body, ex.getStatusCode());
    }
}
