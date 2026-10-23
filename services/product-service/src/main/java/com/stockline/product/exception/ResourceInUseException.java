package com.stockline.product.exception;

import org.springframework.http.HttpStatus;

/**
 * 409 IN_USE: can't delete a row that other rows still point to.
 */
public class ResourceInUseException extends ApiException {

	public ResourceInUseException(String messageKey, Object... messageArgs) {
		super(HttpStatus.CONFLICT, "IN_USE", messageKey, messageArgs);
	}
}
