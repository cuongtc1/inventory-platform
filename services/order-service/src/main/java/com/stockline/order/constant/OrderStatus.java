package com.stockline.order.constant;

import java.util.Set;

/**
 * Where a sales order stands, and which change comes next. This enum is the one place that decides it
 * (docs/conventions.md: behaviour that belongs to the values goes into the enum):
 *
 * <pre>
 * PENDING ──► RESERVED ──► PAID ──► SHIPPED ──► COMPLETED
 *    │            │          │
 *    ├──► REJECTED │          │
 *    └────────────┴──────────┴──► CANCELLED      (only before shipping)
 * </pre>
 *
 * Stored by name in {@code sales_orders.status}. COMPLETED, REJECTED and CANCELLED are final.
 */
public enum OrderStatus {

	/** Created; waiting for the stock reservation (Phase 6). */
	PENDING,
	/** inventory-service holds the stock for every line. */
	RESERVED,
	PAID,
	SHIPPED,
	/** The customer has the goods. */
	COMPLETED,
	/** Not enough stock for at least one line. */
	REJECTED,
	CANCELLED;

	/**
	 * The statuses this one may change to. A {@code switch} expression (Java 14+): every value must be handled, or it
	 * doesn't compile, so a new status can't be forgotten here.
	 */
	public Set<OrderStatus> nextStatuses() {
		return switch (this) {
			case PENDING -> Set.of(RESERVED, REJECTED, CANCELLED);
			case RESERVED -> Set.of(PAID, CANCELLED);
			case PAID -> Set.of(SHIPPED, CANCELLED);
			case SHIPPED -> Set.of(COMPLETED);
			case COMPLETED, REJECTED, CANCELLED -> Set.of();
		};
	}

	public boolean canChangeTo(OrderStatus next) {
		return nextStatuses().contains(next);
	}

	/** Nothing comes after it. */
	public boolean isFinal() {
		return nextStatuses().isEmpty();
	}
}
