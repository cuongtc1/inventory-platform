package com.stockline.product.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Turns every exception into the JSON error format from docs/conventions.md section 3 (Problem Details, RFC 9457).
 *
 * <ul>
 *   <li>Our own {@link ApiException}s carry their status and code.</li>
 *   <li>Spring MVC's errors (405, 415, unknown URL, ...) are handled by {@link ResponseEntityExceptionHandler};
 *       {@link #handleExceptionInternal} adds our fields to them. A few get a clearer message below.</li>
 *   <li>Database constraint errors that slip past the service checks (two requests at the same moment)
 *       become 409 instead of 500.</li>
 *   <li>Anything else is a 500 with a generic message; the details go only to the log.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// Unique keys from the Flyway migrations and the message for each
	private static final Map<String, String> UNIQUE_KEY_MESSAGES = Map.of(
			"uk_products_sku", "A product with this SKU already exists",
			"uk_categories_name", "A category with this name already exists",
			"uk_suppliers_name", "A supplier with this name already exists");

	// MySQL error code for "Cannot delete or update a parent row: a foreign key constraint fails"
	private static final int MYSQL_ROW_IS_REFERENCED = 1451;

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ProblemDetail> handleApiException(ApiException ex, HttpServletRequest request) {
		return response(ex.getStatus(), ex.getMessage(), ex.getCode(), request.getRequestURI());
	}

	/**
	 * Unknown sort field, e.g. {@code ?sort=color,asc}. Spring Data throws this when it builds the query.
	 */
	@ExceptionHandler(PropertyReferenceException.class)
	ResponseEntity<ProblemDetail> handleUnknownSortField(PropertyReferenceException ex, HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, "Unknown sort field: " + ex.getPropertyName(), "BAD_REQUEST",
				request.getRequestURI());
	}

	/**
	 * A unique key or foreign key rejected the change. The services check these rules first and give a precise
	 * message; this only happens when two requests pass the check at the same moment, e.g. both create SKU "A01".
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		String path = request.getRequestURI();
		ConstraintViolationException violation = findCause(ex, ConstraintViolationException.class);
		if (violation == null) {
			return handleUnexpected(ex, request);
		}

		String constraint = violation.getConstraintName() == null ? "" : violation.getConstraintName();
		return switch (violation.getKind()) {
			case UNIQUE -> response(HttpStatus.CONFLICT, uniqueKeyMessage(constraint), "DUPLICATE", path);
			case FOREIGN_KEY -> violation.getErrorCode() == MYSQL_ROW_IS_REFERENCED
					? response(HttpStatus.CONFLICT, "This record is still used by other records", "IN_USE", path)
					// The other direction: the row we point to (category, supplier) was deleted meanwhile
					: response(HttpStatus.NOT_FOUND, "A referenced record no longer exists", "NOT_FOUND", path);
			default -> handleUnexpected(ex, request);
		};
	}

	/**
	 * Last resort for bugs and outages (database down, ...). The client gets no internal details.
	 */
	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", "INTERNAL_ERROR",
				request.getRequestURI());
	}

	/**
	 * {@code @Valid} failed on a request body, or query parameters couldn't be bound to an object.
	 * Lists every invalid field.
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		// Also thrown when a query parameter can't be converted, e.g. ?status=WRONG for the product filter.
		// That error's default message is a long Java exception text, so it gets a short one instead
		List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldValidationError(error.getField(), error.isBindingFailure()
						? invalidValue(error.getRejectedValue())
						: error.getDefaultMessage()))
				.toList();
		return validationFailed(errors, path(request));
	}

	/**
	 * The body couldn't be turned into the request object. A field with a value of the wrong type
	 * ({@code "categoryId": "abc"}, an unknown enum value) is reported like a validation error, so the
	 * frontend can show it next to the field. Broken or missing JSON is a plain 400.
	 */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		// Pattern matching for instanceof (Java 16): tests the type and declares the variable in one step
		if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
			String message = mismatch instanceof InvalidFormatException invalid
					? invalidValue(invalid.getValue())
					: "has a value of the wrong type";
			return validationFailed(List.of(new FieldValidationError(fieldPath(mismatch), message)), path(request));
		}
		return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST,
				"Request body is missing or is not valid JSON", "BAD_REQUEST", path(request)));
	}

	/**
	 * A path variable or simple query parameter has the wrong type, e.g. {@code /api/products/abc}.
	 */
	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String name = ex instanceof MethodArgumentTypeMismatchException argument ? argument.getName()
				: ex.getPropertyName();
		String detail = "Parameter '" + name + "' " + invalidValue(ex.getValue());
		return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, detail, "BAD_REQUEST", path(request)));
	}

	/**
	 * Unknown URL. Spring's own message ("No static resource api/unknown.") talks about files, not endpoints.
	 */
	@Override
	protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String detail = "No endpoint " + ex.getHttpMethod() + " " + path(request);
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(problem(HttpStatus.NOT_FOUND, detail, "NOT_FOUND", path(request)));
	}

	/**
	 * Every other Spring MVC error passes through here with Spring's own Problem Details body
	 * (405 method not allowed, 415 unsupported media type, 404 unknown URL, ...). Add our fields to it.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
		if (response != null && response.getBody() instanceof ProblemDetail problem) {
			if (statusCode.is5xxServerError()) {
				log.error("Unexpected error on {}", path(request), ex);
				problem.setDetail("An unexpected error occurred");
			}
			addStandardFields(problem, codeFor(statusCode), path(request));
		}
		return response;
	}

	private static ResponseEntity<Object> validationFailed(List<FieldValidationError> errors, String path) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed", "VALIDATION_FAILED", path);
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	private static ResponseEntity<ProblemDetail> response(HttpStatus status, String detail, String code, String path) {
		return ResponseEntity.status(status).body(problem(status, detail, code, path));
	}

	private static ProblemDetail problem(HttpStatus status, String detail, String code, String path) {
		return addStandardFields(ProblemDetail.forStatusAndDetail(status, detail), code, path);
	}

	private static ProblemDetail addStandardFields(ProblemDetail problem, String code, String path) {
		// Spring leaves "type" out of the JSON while it has the default value; our convention always includes it
		problem.setType(URI.create("about:blank"));
		problem.setInstance(URI.create(path));
		problem.setProperty("code", code);
		problem.setProperty("timestamp", Instant.now());
		return problem;
	}

	/**
	 * Spring MVC errors use the HTTP status name as code (METHOD_NOT_ALLOWED, UNSUPPORTED_MEDIA_TYPE, ...),
	 * except server errors, which are all INTERNAL_ERROR.
	 */
	private static String codeFor(HttpStatusCode statusCode) {
		if (statusCode.is5xxServerError()) {
			return "INTERNAL_ERROR";
		}
		HttpStatus status = HttpStatus.resolve(statusCode.value());
		return status == null ? "ERROR" : status.name();
	}

	private static String uniqueKeyMessage(String constraint) {
		// MySQL may prefix the key with the table name ("products.uk_products_sku"), so match by "contains"
		return UNIQUE_KEY_MESSAGES.entrySet().stream()
				.filter(entry -> constraint.contains(entry.getKey()))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElse("A record with this value already exists");
	}

	private static String invalidValue(Object value) {
		return "has an invalid value '" + value + "'";
	}

	/**
	 * Jackson's path to the bad value, e.g. {@code status} or, for nested data, {@code items[0].quantity}.
	 */
	private static String fieldPath(JacksonException ex) {
		StringBuilder path = new StringBuilder();
		for (JacksonException.Reference reference : ex.getPath()) {
			if (reference.getPropertyName() != null) {
				if (!path.isEmpty()) {
					path.append('.');
				}
				path.append(reference.getPropertyName());
			} else if (reference.getIndex() >= 0) {
				path.append('[').append(reference.getIndex()).append(']');
			}
		}
		return path.toString();
	}

	private static String path(WebRequest request) {
		return ((ServletWebRequest) request).getRequest().getRequestURI();
	}

	private static <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
		for (Throwable current = ex; current != null; current = current.getCause()) {
			if (type.isInstance(current)) {
				return type.cast(current);
			}
		}
		return null;
	}
}
