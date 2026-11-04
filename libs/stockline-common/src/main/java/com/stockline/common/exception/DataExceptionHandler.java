package com.stockline.common.exception;

import com.stockline.common.constant.CommonMessageKeys;
import com.stockline.common.constant.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The errors only services with a database have. Separate from {@link GlobalExceptionHandler} because these
 * methods name Hibernate and Spring Data classes: a service without them (the api-gateway) couldn't even load a
 * class that mentions them. {@code CommonWebAutoConfiguration} creates this bean only when they are present.
 *
 * <p>{@code @Order}: Spring MVC asks each {@code @RestControllerAdvice} in turn and stops at the first one with a
 * matching method. {@link GlobalExceptionHandler} matches everything ({@code Exception.class}), so this one must
 * come first.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataExceptionHandler {

	// MySQL error code for "Cannot delete or update a parent row: a foreign key constraint fails"
	private static final int MYSQL_ROW_IS_REFERENCED = 1451;

	private final GlobalExceptionHandler handler;
	private final MessageSource messageSource;

	public DataExceptionHandler(GlobalExceptionHandler handler, MessageSource messageSource) {
		this.handler = handler;
		this.messageSource = messageSource;
	}

	/**
	 * Unknown sort field, e.g. {@code ?sort=color,asc}. Spring Data throws this when it builds the query.
	 */
	@ExceptionHandler(PropertyReferenceException.class)
	public ResponseEntity<ProblemDetail> handleUnknownSortField(PropertyReferenceException ex,
			HttpServletRequest request) {
		return handler.response(ErrorCode.BAD_REQUEST,
				handler.message(CommonMessageKeys.REQUEST_UNKNOWN_SORT_FIELD, ex.getPropertyName()),
				request.getRequestURI());
	}

	/**
	 * A unique key or foreign key rejected the change. The services check these rules first and give a precise
	 * message; this only happens when two requests pass the check at the same moment, e.g. both create SKU "A01".
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		String path = request.getRequestURI();
		ConstraintViolationException violation = findCause(ex, ConstraintViolationException.class);
		if (violation == null) {
			return handler.handleUnexpected(ex, request);
		}

		String constraint = violation.getConstraintName() == null ? "" : violation.getConstraintName();
		return switch (violation.getKind()) {
			case UNIQUE -> handler.response(ErrorCode.DUPLICATE, uniqueKeyMessage(constraint), path);
			case FOREIGN_KEY -> violation.getErrorCode() == MYSQL_ROW_IS_REFERENCED
					? handler.response(ErrorCode.IN_USE, handler.message(CommonMessageKeys.RESOURCE_IN_USE), path)
					// The other direction: the row we point to (category, supplier) was deleted meanwhile
					: handler.response(ErrorCode.NOT_FOUND,
							handler.message(CommonMessageKeys.RESOURCE_REFERENCE_MISSING), path);
			default -> handler.handleUnexpected(ex, request);
		};
	}

	/**
	 * The library doesn't know the services' tables, so each service names the text for its unique keys in its own
	 * messages.properties: {@code constraint.uk_products_sku=...}. Without such a line, the common text is used.
	 */
	private String uniqueKeyMessage(String constraint) {
		// MySQL may prefix the key with the table name ("products.uk_products_sku")
		String keyName = constraint.substring(constraint.lastIndexOf('.') + 1);
		String fallback = handler.message(CommonMessageKeys.RESOURCE_DUPLICATE);
		return messageSource.getMessage(CommonMessageKeys.CONSTRAINT_PREFIX + keyName, null, fallback,
				LocaleContextHolder.getLocale());
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
