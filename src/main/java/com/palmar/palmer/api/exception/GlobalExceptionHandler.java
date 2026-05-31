package com.palmar.palmer.api.exception;

import com.palmar.palmer.api.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException ex,
                                                           HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDTO.of(404, "Not Found", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponseDTO.of(401, "Unauthorized", "Credenciales inválidas", request.getRequestURI()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponseDTO.of(403, "Forbidden", "Acceso denegado", request.getRequestURI()));
    }

    // Errores de validación @Valid — recolecta todos los campos inválidos
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.of(400, "Bad Request", "Error de validación", request.getRequestURI(), details));
    }

    // Violaciones en @PathVariable / @RequestParam con @Validated
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(ConstraintViolationException ex,
                                                                       HttpServletRequest request) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> {
                    String field = v.getPropertyPath().toString();
                    // Quita el prefijo del método (ej: "findById.codArticulo" → "codArticulo")
                    int dot = field.lastIndexOf('.');
                    return (dot >= 0 ? field.substring(dot + 1) : field) + ": " + v.getMessage();
                })
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.of(400, "Bad Request", "Parámetro inválido", request.getRequestURI(), details));
    }

    // Query param requerido ausente (ej: /search sin ?nombre=)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingParam(MissingServletRequestParameterException ex,
                                                               HttpServletRequest request) {
        String message = String.format("El parámetro '%s' es requerido", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.of(400, "Bad Request", message, request.getRequestURI()));
    }

    // Tipo de parámetro incorrecto (ej: página con letras en vez de número)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest request) {
        String message = String.format("El parámetro '%s' tiene un valor inválido: '%s'", ex.getName(), ex.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.of(400, "Bad Request", message, request.getRequestURI()));
    }

    // Cliente desconectado antes de que termine la escritura del JSON — no es un error del servidor
    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotWritable(HttpMessageNotWritableException ex,
                                                              HttpServletRequest request) {
        if (hasClientAbortCause(ex)) {
            log.debug("Client disconnected mid-response for {}", request.getRequestURI());
            return ResponseEntity.noContent().build();
        }
        log.error("Error escribiendo respuesta en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDTO.of(500, "Internal Server Error", "Error interno del servidor", request.getRequestURI()));
    }

    private static boolean hasClientAbortCause(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof AsyncRequestNotUsableException) return true;
            t = t.getCause();
        }
        return false;
    }

    // Fallback — cualquier error no contemplado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDTO.of(500, "Internal Server Error", "Error interno del servidor", request.getRequestURI()));
    }
}
