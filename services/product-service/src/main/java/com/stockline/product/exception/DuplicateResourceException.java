package com.stockline.product.exception;

import org.springframework.http.HttpStatus;

/**
 * 409 DUPLICATE: a value that must be unique already exists, e.g. a category name.
 */
public class DuplicateResourceException extends ApiException {

	public DuplicateResourceException(String message) {
		super(HttpStatus.CONFLICT, "DUPLICATE", message);
	}
}
