package com.stockline.product.exception;

import com.stockline.product.constant.ErrorCode;

/**
 * 409 IN_USE: can't delete a row that other rows still point to.
 */
public class ResourceInUseException extends ApiException {

	public ResourceInUseException(String messageKey, Object... messageArgs) {
		super(ErrorCode.IN_USE, messageKey, messageArgs);
	}
}
