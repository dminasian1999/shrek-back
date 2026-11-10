package dev.shrekback.security;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("status", error.getStatusCode().value(), "message",
                error.getReason() == null ? "The request could not be completed." : error.getReason()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> badRequest(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("status", 400, "message", "Please check the submitted information and try again."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> other(Exception error) {
        ResponseStatus annotation = AnnotatedElementUtils.findMergedAnnotation(error.getClass(), ResponseStatus.class);
        HttpStatus status = annotation == null ? HttpStatus.INTERNAL_SERVER_ERROR : annotation.code();
        String message = switch (status) {
            case CONFLICT -> "An account with this email already exists.";
            case NOT_FOUND -> "The requested resource was not found.";
            case FORBIDDEN -> "This link has expired or is no longer valid.";
            case BAD_REQUEST -> "Please check the submitted information and try again.";
            default -> "The service is temporarily unavailable. Please try again.";
        };
        return ResponseEntity.status(status).body(Map.of("status", status.value(), "message", message));
    }
}
