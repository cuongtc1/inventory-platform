package com.stockline.common.exception;

import com.stockline.common.constant.ErrorCode;

/**
 * 400 BAD_REQUEST: input that passes Bean Validation but breaks a business rule,
 * e.g. making a category its own parent.
 */
public class InvalidRequestException extends ApiException {

	public InvalidRequestException(String messageKey, Object... messageArgs) {
		super(ErrorCode.BAD_REQUEST, messageKey, messageArgs);
	}
}
