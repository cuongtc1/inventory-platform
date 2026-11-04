package com.stockline.common.exception;

import com.stockline.common.constant.ErrorCode;
import com.stockline.common.constant.CommonMessageKeys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.net.URI;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;

/**
 * Turns every exception into the JSON error format from docs/conventions.md section 3 (Problem Details, RFC 9457).
 *
 * <ul>
 *   <li>Our own {@link ApiException}s carry their {@link ErrorCode} and message key.</li>
 *   <li>Spring MVC's errors (405, 415, unknown URL, ...) are handled by {@link ResponseEntityExceptionHandler};
 *       {@link #handleExceptionInternal} adds our fields to them. A few get a clearer message below.</li>
 *   <li>Database errors are in {@link DataExceptionHandler}, because not every service has a database.</li>
 *   <li>Anything else is a 500 with a generic message; the details go only to the log.</li>
 * </ul>
 *
 * <p>Every text comes from the messages files through {@link MessageSource}; the timestamp comes from the
 * {@link Clock} bean, so tests can fix the time.
 *
 * <p>Not a component found by scanning: services don't scan {@code com.stockline.common}.
 * {@code CommonWebAutoConfiguration} creates it as a bean, and the {@code @RestControllerAdvice} annotation
 * then makes Spring MVC use it for every controller.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private final MessageSource messageSource;
	private final Clock clock;

	public GlobalExceptionHandler(MessageSource messageSource, Clock clock) {
		this.messageSource = messageSource;
		this.clock = clock;
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ProblemDetail> handleApiException(ApiException ex, HttpServletRequest request) {
		return response(ex.getErrorCode(), message(ex.getMessageKey(), ex.getMessageArgs()), request.getRequestURI());
	}

	/**
	 * A call to another service failed before an answer came back: it is down, or too slow. Thrown by Spring's
	 * HTTP clients, e.g. in the api-gateway. A 503, not a 500: nothing is wrong with the request or with this service.
	 */
	@ExceptionHandler(ResourceAccessException.class)
	public ResponseEntity<ProblemDetail> handleServiceUnavailable(ResourceAccessException ex,
			HttpServletRequest request) {
		log.warn(message(CommonMessageKeys.SYSTEM_ERROR_UNEXPECTED, request.getMethod(), request.getRequestURI())
				+ ": " + ex.getMessage());
		return response(ErrorCode.SERVICE_UNAVAILABLE, message(CommonMessageKeys.ERROR_SERVICE_UNAVAILABLE),
				request.getRequestURI());
	}

	/**
	 * Last resort for bugs and outages (database down, ...). The client gets no internal details.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error(message(CommonMessageKeys.SYSTEM_ERROR_UNEXPECTED, request.getMethod(), request.getRequestURI()), ex);
		return response(ErrorCode.INTERNAL_ERROR, message(CommonMessageKeys.ERROR_UNEXPECTED), request.getRequestURI());
	}

	/**
	 * {@code @Valid} failed on a request body, or query parameters couldn't be bound to an object.
	 * Lists every invalid field. The field messages come from messages.properties through the validator.
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		// Also thrown when a query parameter can't be converted, e.g. ?status=WRONG for the product filter.
		// That error's default message is a long Java exception text, so it gets a short one instead
		List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldValidationError(error.getField(), error.isBindingFailure()
						? message(CommonMessageKeys.VALIDATION_INVALID_VALUE, error.getRejectedValue())
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
			String text = mismatch instanceof InvalidFormatException invalid
					? message(CommonMessageKeys.VALIDATION_INVALID_VALUE, invalid.getValue())
					: message(CommonMessageKeys.VALIDATION_WRONG_TYPE);
			return validationFailed(List.of(new FieldValidationError(fieldPath(mismatch), text)), path(request));
		}
		return objectResponse(ErrorCode.BAD_REQUEST, message(CommonMessageKeys.REQUEST_UNREADABLE), path(request));
	}

	/**
	 * A path variable or simple query parameter has the wrong type, e.g. {@code /api/products/abc}.
	 */
	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String name = ex instanceof MethodArgumentTypeMismatchException argument ? argument.getName()
				: ex.getPropertyName();
		String detail = message(CommonMessageKeys.REQUEST_INVALID_PARAMETER, name, ex.getValue());
		return objectResponse(ErrorCode.BAD_REQUEST, detail, path(request));
	}

	/**
	 * Unknown URL. Spring's own message ("No static resource api/unknown.") talks about files, not endpoints.
	 */
	@Override
	protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String detail = message(CommonMessageKeys.REQUEST_NO_ENDPOINT, ex.getHttpMethod(), path(request));
		return objectResponse(ErrorCode.NOT_FOUND, detail, path(request));
	}

	/**
	 * Every other Spring MVC error passes through here with Spring's own Problem Details body
	 * (405 method not allowed, 415 unsupported media type, ...). Spring takes its detail text from
	 * messages.properties too (keys "problemDetail.&lt;exception class&gt;"). Add our fields to it.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
		if (response != null && response.getBody() instanceof ProblemDetail problem) {
			if (statusCode.is5xxServerError()) {
				String method = ((ServletWebRequest) request).getRequest().getMethod();
				log.error(message(CommonMessageKeys.SYSTEM_ERROR_UNEXPECTED, method, path(request)), ex);
				problem.setDetail(message(CommonMessageKeys.ERROR_UNEXPECTED));
			}
			addStandardFields(problem, codeFor(statusCode), path(request));
		}
		return response;
	}

	/**
	 * Looks up a text in messages.properties, in the request's language (only English exists for now).
	 * Numbers are passed as text: MessageFormat would otherwise print the id 1234 as "1,234".
	 */
	String message(String key, Object... args) {
		Object[] textArgs = Arrays.stream(args)
				.map(arg -> arg instanceof Number ? arg.toString() : arg)
				.toArray();
		return messageSource.getMessage(key, textArgs, LocaleContextHolder.getLocale());
	}

	private ResponseEntity<Object> validationFailed(List<FieldValidationError> errors, String path) {
		ProblemDetail problem = problem(ErrorCode.VALIDATION_FAILED, message(CommonMessageKeys.VALIDATION_FAILED), path);
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	ResponseEntity<ProblemDetail> response(ErrorCode code, String detail, String path) {
		return ResponseEntity.status(code.getStatus()).body(problem(code, detail, path));
	}

	// Same as response(), typed for the ResponseEntityExceptionHandler methods, which return ResponseEntity<Object>
	private ResponseEntity<Object> objectResponse(ErrorCode code, String detail, String path) {
		return ResponseEntity.status(code.getStatus()).body(problem(code, detail, path));
	}

	private ProblemDetail problem(ErrorCode code, String detail, String path) {
		return addStandardFields(ProblemDetail.forStatusAndDetail(code.getStatus(), detail), code.name(), path);
	}

	private ProblemDetail addStandardFields(ProblemDetail problem, String code, String path) {
		// Spring leaves "type" out of the JSON while it has the default value; our convention always includes it
		problem.setType(URI.create("about:blank"));
		problem.setInstance(URI.create(path));
		problem.setProperty("code", code);
		problem.setProperty("timestamp", clock.instant());
		return problem;
	}

	/**
	 * Spring MVC errors use our {@link ErrorCode} when one has the status name (METHOD_NOT_ALLOWED, ...),
	 * otherwise the HTTP status name; server errors are all INTERNAL_ERROR.
	 */
	private static String codeFor(HttpStatusCode statusCode) {
		if (statusCode.is5xxServerError()) {
			return ErrorCode.INTERNAL_ERROR.name();
		}
		HttpStatus status = HttpStatus.resolve(statusCode.value());
		return status == null ? ErrorCode.BAD_REQUEST.name() : status.name();
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
}
