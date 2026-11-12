package com.stockline.inventory.event;

import java.util.List;

/**
 * Payload of {@code order.created}, as inventory-service reads it. order-service publishes it; this record declares
 * only what inventory uses, and other fields are ignored.
 */
public record OrderCreatedEvent(
		long orderId,
		String orderNumber,
		long warehouseId,
		List<Item> items
) {

	/** One line. The name tells people which product was short, even one this warehouse has never stocked. */
	public record Item(long productId, String productName, int quantity) {
	}
}
