package com.stockline.inventory.constant;

/**
 * What caused a stock movement. Stored as a string in {@code stock_movements.movement_type}.
 */
public enum MovementType {
	/** Goods received (purchase orders, Phase 7). */
	IN,
	/** Goods shipped (Phase 6). */
	OUT,
	/** A manual correction, positive or negative. */
	ADJUST,
	/** Stock reserved for an order (Phase 6). */
	RESERVE,
	/** A reservation given back (Phase 6). */
	RELEASE
}
