package com.fides.sienep.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Ataja las validaciones de los DTOs (@NotBlank, @Pattern, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Error de validación en los datos enviados");

        // Recolecta exactamente qué campos fallaron y el mensaje personalizado
        Map<String, String> detalles = new HashMap<>();
        for (var error : ex.getBindingResult().getAllErrors()) {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            detalles.put(fieldName, errorMessage);
        }
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

    // 3B. Ataja archivos que superan spring.servlet.multipart.max-file-size / max-request-size
    // (se dispara al parsear el multipart, antes de llegar al Service)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Archivo demasiado grande");
        body.put("message", "El archivo supera el tamaño máximo permitido de 5MB.");

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 4. Ataja errores de autorización (Spring Security)
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(RuntimeException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Acceso denegado");
        body.put("message", "No tienes permisos suficientes para realizar esta acción.");

        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    // 5. Ataja @PathVariable/@RequestParam con tipo incorrecto (H12), ej.
    // GET /api/carreras/abc con @PathVariable Integer id — sin este handler
    // caía en el genérico de abajo y devolvía 500 en vez de 400.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Parámetro inválido");
        body.put("message", "El parámetro '" + ex.getName() + "' tiene un formato inválido.");

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 6. Ataja violaciones de constraints de BD (H12), ej. UNIQUE, que un
    // service no pudo prevenir con su chequeo previo (existsBy...) por una
    // condición de carrera entre dos requests concurrentes.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflicto de datos");
        body.put("message", "La operación no pudo completarse por un conflicto con datos existentes.");

        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    // 7. Ataja cualquier otra excepción no controlada (ej: NullPointerException, Errores 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Imprime la traza en consola para el desarrollador
        log.error("Excepción no controlada en el servidor: ", ex);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Error interno del servidor");
        body.put("message", "Ocurrió un error inesperado.");

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
