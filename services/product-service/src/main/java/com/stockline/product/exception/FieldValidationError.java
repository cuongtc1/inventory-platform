package com.stockline.product.exception;

/**
 * One entry of {@code errors} in a 400 VALIDATION_FAILED response.
 */
public record FieldValidationError(String field, String message) {
}
