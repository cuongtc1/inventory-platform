package com.stockline.product.exception;

import com.stockline.product.constant.ErrorCode;
import com.stockline.product.constant.MessageKeys;

/**
 * 404 NOT_FOUND: no row with this id. Uses one common message for every resource:
 * "{resource} with id {id} not found".
 */
public class ResourceNotFoundException extends ApiException {

	/**
	 * @param resource the name shown in the message, e.g. {@code Category.class.getSimpleName()}
	 */
	public ResourceNotFoundException(String resource, Long id) {
		super(ErrorCode.NOT_FOUND, MessageKeys.RESOURCE_NOT_FOUND, resource, id);
	}
}
