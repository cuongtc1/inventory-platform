package com.stockline.common.constant;

/**
 * Keys of {@code common-messages.properties} (in this library): texts every service uses.
 * Each service keeps its own keys in its own {@code messages.properties} and {@code MessageKeys} class.
 *
 * <p>Use these constants instead of typing a key, so a typo is a compile error, not a missing message at runtime.
 */
public final class CommonMessageKeys {

	// Validation response
	public static final String VALIDATION_FAILED = "common.validation.failed";
	public static final String VALIDATION_INVALID_VALUE = "common.validation.invalid-value";
	public static final String VALIDATION_WRONG_TYPE = "common.validation.wrong-type";

	// Bad requests
	public static final String REQUEST_UNREADABLE = "common.request.unreadable";
	public static final String REQUEST_INVALID_PARAMETER = "common.request.invalid-parameter";
	public static final String REQUEST_UNKNOWN_SORT_FIELD = "common.request.unknown-sort-field";
	public static final String REQUEST_NO_ENDPOINT = "common.request.no-endpoint";

	// Resources
	public static final String RESOURCE_NOT_FOUND = "common.resource.not-found";
	public static final String RESOURCE_DUPLICATE = "common.resource.duplicate";
	public static final String RESOURCE_IN_USE = "common.resource.in-use";
	public static final String RESOURCE_REFERENCE_MISSING = "common.resource.reference-missing";

	// Server errors
	public static final String ERROR_UNEXPECTED = "common.error.unexpected";

	// System (log only, never sent to the client)
	public static final String SYSTEM_ERROR_UNEXPECTED = "system.error.unexpected";

	/**
	 * Prefix for the message of a database constraint: a service adds {@code constraint.uk_products_sku=...} to its
	 * messages.properties, and the exception handler uses it when that unique key rejects a row.
	 */
	public static final String CONSTRAINT_PREFIX = "constraint.";

	private CommonMessageKeys() {
	}
}
