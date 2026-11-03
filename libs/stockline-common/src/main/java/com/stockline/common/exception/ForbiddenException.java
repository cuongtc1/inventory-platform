package com.stockline.common.exception;

import com.stockline.common.constant.ErrorCode;

/**
 * 403 FORBIDDEN: the caller is signed in, but their role doesn't allow this action
 * (docs/conventions.md section 11).
 */
public class ForbiddenException extends ApiException {

	public ForbiddenException(String messageKey, Object... messageArgs) {
		super(ErrorCode.FORBIDDEN, messageKey, messageArgs);
	}
}
