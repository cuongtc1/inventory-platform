package com.stockline.product.exception;

import org.springframework.http.HttpStatus;

/**
 * 400 BAD_REQUEST: input that passes Bean Validation but breaks a business rule,
 * e.g. making a category its own parent.
 */
public class InvalidRequestException extends ApiException {

    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
