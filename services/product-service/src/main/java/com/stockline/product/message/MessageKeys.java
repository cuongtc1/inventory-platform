package com.stockline.product.message;

/**
 * Keys of {@code src/main/resources/messages.properties}. Use these constants instead of typing a key,
 * so a typo is a compile error instead of a missing message at runtime.
 *
 * <p>Bean Validation keys are not here: annotations reference them directly, e.g.
 * {@code @Pattern(message = "{product.sku.invalid}")}.
 */
public final class MessageKeys {

	// common: validation response
	public static final String VALIDATION_FAILED = "common.validation.failed";
	public static final String VALIDATION_INVALID_VALUE = "common.validation.invalid-value";
	public static final String VALIDATION_WRONG_TYPE = "common.validation.wrong-type";

	// common: bad requests
	public static final String REQUEST_UNREADABLE = "common.request.unreadable";
	public static final String REQUEST_INVALID_PARAMETER = "common.request.invalid-parameter";
	public static final String REQUEST_UNKNOWN_SORT_FIELD = "common.request.unknown-sort-field";
	public static final String REQUEST_NO_ENDPOINT = "common.request.no-endpoint";

	// common: resources
	public static final String RESOURCE_NOT_FOUND = "common.resource.not-found";
	public static final String RESOURCE_DUPLICATE = "common.resource.duplicate";
	public static final String RESOURCE_IN_USE = "common.resource.in-use";
	public static final String RESOURCE_REFERENCE_MISSING = "common.resource.reference-missing";

	// common: server errors
	public static final String ERROR_UNEXPECTED = "common.error.unexpected";

	// category
	public static final String CATEGORY_NAME_DUPLICATE = "category.name.duplicate";
	public static final String CATEGORY_NAME_DUPLICATE_CONCURRENT = "category.name.duplicate-concurrent";
	public static final String CATEGORY_PARENT_LOOP = "category.parent.loop";
	public static final String CATEGORY_DELETE_HAS_SUBCATEGORIES = "category.delete.has-subcategories";
	public static final String CATEGORY_DELETE_HAS_PRODUCTS = "category.delete.has-products";

	// supplier
	public static final String SUPPLIER_NAME_DUPLICATE = "supplier.name.duplicate";
	public static final String SUPPLIER_NAME_DUPLICATE_CONCURRENT = "supplier.name.duplicate-concurrent";
	public static final String SUPPLIER_DELETE_HAS_PRODUCTS = "supplier.delete.has-products";

	// product
	public static final String PRODUCT_SKU_DUPLICATE = "product.sku.duplicate";
	public static final String PRODUCT_SKU_DUPLICATE_CONCURRENT = "product.sku.duplicate-concurrent";

	// system (log only)
	public static final String SYSTEM_ERROR_UNEXPECTED = "system.error.unexpected";

	private MessageKeys() {
	}
}
