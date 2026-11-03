package com.stockline.common.exception;

import com.stockline.common.constant.ErrorCode;

/**
 * 401 UNAUTHORIZED: the caller is not (or no longer) signed in, e.g. a wrong email or password at login,
 * or a missing, invalid or expired token.
 */
public class UnauthorizedException extends ApiException {

	public UnauthorizedException(String messageKey, Object... messageArgs) {
		super(ErrorCode.UNAUTHORIZED, messageKey, messageArgs);
	}
}
