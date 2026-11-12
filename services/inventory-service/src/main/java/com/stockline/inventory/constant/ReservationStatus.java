package com.stockline.inventory.constant;

/**
 * Where a stock reservation stands. Stored as a string in {@code stock_reservations.status}.
 */
public enum ReservationStatus {
	/** The stock is set aside for the order: on hand, but no longer available. */
	RESERVED,
	/** The order shipped: the stock left the warehouse (T6.6). */
	CONFIRMED,
	/** The order was cancelled: the stock is available again (T6.6). */
	RELEASED
}
