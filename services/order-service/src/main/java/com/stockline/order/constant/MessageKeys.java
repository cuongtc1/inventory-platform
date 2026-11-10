package com.stockline.order.constant;

/**
 * Keys of order-service's {@code src/main/resources/messages.properties}. Use these constants instead of typing
 * a key, so a typo is a compile error instead of a missing message at runtime.
 *
 * <p>Texts every service shares (validation, not found, 401/403, server errors, ...) are in the stockline-common
 * library: {@code com.stockline.common.constant.CommonMessageKeys}.
 */
public final class MessageKeys {

	// customer
	public static final String CUSTOMER_NAME_DUPLICATE = "customer.name.duplicate";
	public static final String CUSTOMER_DELETE_HAS_ORDERS = "customer.delete.has-orders";

	// order
	public static final String ORDER_PRODUCT_TWICE = "order.product.twice";
	public static final String ORDER_PRODUCT_INACTIVE = "order.product.inactive";
	public static final String ORDER_TOTAL_TOO_LARGE = "order.total.too-large";
	public static final String ORDER_STATUS_CHANGE_NOT_ALLOWED = "order.status.change-not-allowed";

	private MessageKeys() {
	}
}
