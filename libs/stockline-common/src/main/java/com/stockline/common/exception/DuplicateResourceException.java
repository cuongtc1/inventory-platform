package com.stockline.common.exception;

import com.stockline.common.constant.ErrorCode;

/**
 * 409 DUPLICATE: a value that must be unique already exists, e.g. a category name.
 */
public class DuplicateResourceException extends ApiException {

	public DuplicateResourceException(String messageKey, Object... messageArgs) {
		super(ErrorCode.DUPLICATE, messageKey, messageArgs);
	}
}
