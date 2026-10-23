package com.stockline.product.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for errors the service throws on purpose. Each subclass fixes the HTTP status and the
 * {@code code} from docs/conventions.md section 3; {@link GlobalExceptionHandler} turns it into the JSON error.
 */
public abstract class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	protected ApiException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
