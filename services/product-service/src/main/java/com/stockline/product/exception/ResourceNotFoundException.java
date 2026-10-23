package com.stockline.product.exception;

import org.springframework.http.HttpStatus;

/**
 * 404 NOT_FOUND: no row with this id.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource, Long id) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", resource + " with id " + id + " not found");
    }
}
