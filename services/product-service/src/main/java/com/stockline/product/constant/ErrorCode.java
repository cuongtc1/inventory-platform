package com.stockline.product.constant;

import org.springframework.http.HttpStatus;

/**
 * The {@code code} field of every error response (docs/conventions.md section 3), with its HTTP status.
 * The frontend checks this code, never the text. Errors raised by Spring MVC itself that have no constant here
 * (e.g. 413) use the HTTP status name as code.
 */
public enum ErrorCode {

	VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
	BAD_REQUEST(HttpStatus.BAD_REQUEST),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
	FORBIDDEN(HttpStatus.FORBIDDEN),
	NOT_FOUND(HttpStatus.NOT_FOUND),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
	NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
	DUPLICATE(HttpStatus.CONFLICT),
	IN_USE(HttpStatus.CONFLICT),
	INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT),
	CONCURRENT_UPDATE(HttpStatus.CONFLICT),
	INSUFFICIENT_STOCK(HttpStatus.CONFLICT),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

	// Enums can have fields and a constructor; each constant above calls it with its own status
	private final HttpStatus status;

	ErrorCode(HttpStatus status) {
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
