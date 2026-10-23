package com.stockline.common.exception;

import com.stockline.common.constant.ErrorCode;
import com.stockline.common.constant.CommonMessageKeys;

/**
 * 404 NOT_FOUND: no row with this id. Uses one common message for every resource:
 * "{resource} with id {id} not found".
 */
public class ResourceNotFoundException extends ApiException {

	/**
	 * @param resource the name shown in the message, e.g. {@code Category.class.getSimpleName()}
	 */
	public ResourceNotFoundException(String resource, Long id) {
		super(ErrorCode.NOT_FOUND, CommonMessageKeys.RESOURCE_NOT_FOUND, resource, id);
	}
}
