package com.stockline.order.constant;

/**
 * Where a sales order stands: {@code PENDING → RESERVED → PAID → SHIPPED → COMPLETED}, plus {@code REJECTED} (not
 * enough stock) and {@code CANCELLED} (only before shipping). Stored by name in {@code sales_orders.status}.
 *
 * <p>Which change is allowed comes with the state machine in T5.4, here in the enum
 * (docs/conventions.md: behaviour that belongs to the values goes into the enum).
 */
public enum OrderStatus {

	/** Created; waiting for the stock reservation (Phase 6). */
	PENDING,
	/** inventory-service holds the stock for every line. */
	RESERVED,
	PAID,
	SHIPPED,
	COMPLETED,
	/** Not enough stock for at least one line. */
	REJECTED,
	CANCELLED
}
