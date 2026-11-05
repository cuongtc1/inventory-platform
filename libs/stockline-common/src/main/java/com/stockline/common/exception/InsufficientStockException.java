package com.stockline.common.exception;

import com.stockline.common.constant.ErrorCode;

/**
 * 409 INSUFFICIENT_STOCK: the change would take more stock than is available.
 */
public class InsufficientStockException extends ApiException {

	public InsufficientStockException(String messageKey, Object... messageArgs) {
		super(ErrorCode.INSUFFICIENT_STOCK, messageKey, messageArgs);
	}
}
