package com.stockline.inventory.constant;

/**
 * Keys of inventory-service's {@code src/main/resources/messages.properties}. Use these constants instead of typing
 * a key, so a typo is a compile error instead of a missing message at runtime.
 *
 * <p>Texts every service shares (validation, not found, 401/403, server errors, ...) are in the stockline-common
 * library: {@code com.stockline.common.constant.CommonMessageKeys}.
 */
public final class MessageKeys {

	// warehouse
	public static final String WAREHOUSE_CODE_DUPLICATE = "warehouse.code.duplicate";
	public static final String WAREHOUSE_DELETE_HAS_STOCK = "warehouse.delete.has-stock";

	private MessageKeys() {
	}
}
