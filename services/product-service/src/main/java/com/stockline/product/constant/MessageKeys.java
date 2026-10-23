package com.stockline.product.constant;

/**
 * Keys of product-service's {@code src/main/resources/messages.properties}. Use these constants instead of typing
 * a key, so a typo is a compile error instead of a missing message at runtime.
 *
 * <p>Texts every service shares (validation, not found, server errors, ...) are in the stockline-common library:
 * {@code com.stockline.common.constant.CommonMessageKeys}.
 */
public final class MessageKeys {

	// category
	public static final String CATEGORY_NAME_DUPLICATE = "category.name.duplicate";
	public static final String CATEGORY_PARENT_LOOP = "category.parent.loop";
	public static final String CATEGORY_DELETE_HAS_SUBCATEGORIES = "category.delete.has-subcategories";
	public static final String CATEGORY_DELETE_HAS_PRODUCTS = "category.delete.has-products";

	// supplier
	public static final String SUPPLIER_NAME_DUPLICATE = "supplier.name.duplicate";
	public static final String SUPPLIER_DELETE_HAS_PRODUCTS = "supplier.delete.has-products";

	// product
	public static final String PRODUCT_SKU_DUPLICATE = "product.sku.duplicate";

	private MessageKeys() {
	}
}
