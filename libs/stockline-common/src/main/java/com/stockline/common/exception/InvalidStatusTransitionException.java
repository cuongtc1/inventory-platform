package com.stockline.common.exception;

import com.stockline.common.constant.ErrorCode;

/**
 * 409 INVALID_STATUS_TRANSITION: the record's current status doesn't allow this change, e.g. shipping an order that
 * isn't paid. The rules themselves live in the service's status enum.
 */
public class InvalidStatusTransitionException extends ApiException {

	public InvalidStatusTransitionException(String messageKey, Object... messageArgs) {
		super(ErrorCode.INVALID_STATUS_TRANSITION, messageKey, messageArgs);
	}
}
