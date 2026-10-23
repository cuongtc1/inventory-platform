package com.stockline.product.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Turns exceptions into the JSON error format from docs/conventions.md section 3 (Problem Details, RFC 9457).
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} gives Problem Details for Spring MVC's own errors
 * (unreadable JSON, unsupported method, ...). The methods here add our {@code code} and {@code timestamp} fields.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException ex, HttpServletRequest request) {
        ProblemDetail problem = problem(ex.getStatus(), ex.getMessage(), ex.getCode(), request.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    /**
     * Unknown sort field, e.g. {@code ?sort=color,asc}. Spring Data throws this when it builds the query.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    ResponseEntity<ProblemDetail> handleUnknownSortField(PropertyReferenceException ex, HttpServletRequest request) {
        String detail = "Unknown sort field: " + ex.getPropertyName();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, detail, "BAD_REQUEST", request.getRequestURI());
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * {@code @Valid} failed on a request body. Lists every invalid field.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
                .toList();

        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed", "VALIDATION_FAILED", path);
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    private static ProblemDetail problem(HttpStatus status, String detail, String code, String path) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        // Spring leaves "type" out of the JSON while it has the default value; our convention always includes it
        problem.setType(URI.create("about:blank"));
        problem.setInstance(URI.create(path));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
