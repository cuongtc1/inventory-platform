package com.stockline.product.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for errors the service throws on purpose. Each subclass fixes the HTTP status and the
 * {@code code} from docs/conventions.md section 3.
 *
 * <p>The exception carries a message key from messages.properties and its arguments, not the final text;
 * {@link GlobalExceptionHandler} looks the text up. That keeps services free of message lookups and
 * lets the text depend on the request's language later. {@link #getMessage()} returns the key (for logs).
 */
public abstract class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final String messageKey;
	private final transient Object[] messageArgs;

	protected ApiException(HttpStatus status, String code, String messageKey, Object... messageArgs) {
		super(messageKey);
		this.status = status;
		this.code = code;
		this.messageKey = messageKey;
		this.messageArgs = messageArgs;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public String getMessageKey() {
		return messageKey;
	}

	public Object[] getMessageArgs() {
		return messageArgs.clone();
	}
}
