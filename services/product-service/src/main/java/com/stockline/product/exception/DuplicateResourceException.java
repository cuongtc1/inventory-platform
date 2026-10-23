package com.stockline.product.exception;

import com.stockline.product.constant.ErrorCode;

/**
 * 409 DUPLICATE: a value that must be unique already exists, e.g. a category name.
 */
public class DuplicateResourceException extends ApiException {

	public DuplicateResourceException(String messageKey, Object... messageArgs) {
		super(ErrorCode.DUPLICATE, messageKey, messageArgs);
	}
}
