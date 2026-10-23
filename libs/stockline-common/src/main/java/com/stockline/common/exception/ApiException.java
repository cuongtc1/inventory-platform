package com.stockline.common.exception;

import com.stockline.common.constant.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Base class for errors the service throws on purpose. Each subclass fixes the {@link ErrorCode},
 * which also decides the HTTP status.
 *
 * <p>The exception carries a message key from messages.properties and its arguments, not the final text;
 * {@link GlobalExceptionHandler} looks the text up. That keeps services free of message lookups and
 * lets the text depend on the request's language later. {@link #getMessage()} returns the key (for logs).
 */
public abstract class ApiException extends RuntimeException {

	private final ErrorCode errorCode;
	private final String messageKey;
	private final transient Object[] messageArgs;

	protected ApiException(ErrorCode errorCode, String messageKey, Object... messageArgs) {
		super(messageKey);
		this.errorCode = errorCode;
		this.messageKey = messageKey;
		this.messageArgs = messageArgs;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public HttpStatus getStatus() {
		return errorCode.getStatus();
	}

	public String getMessageKey() {
		return messageKey;
	}

	public Object[] getMessageArgs() {
		return messageArgs.clone();
	}
}
