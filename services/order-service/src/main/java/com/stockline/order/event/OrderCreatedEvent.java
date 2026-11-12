package com.stockline.order.event;

import java.util.List;

/**
 * Payload of {@code order.created}: an order was created and needs stock. Published by order-service; inventory-service
 * declares its own record with the fields it reads. Key: the order id.
 *
 * @param orderNumber for people: inventory uses it as the reference of its stock movements
 * @param warehouseId where the stock is to be reserved
 */
public record OrderCreatedEvent(
		long orderId,
		String orderNumber,
		long warehouseId,
		List<Item> items
) {

	/** One line. SKU and name let inventory name a product it has never stocked. */
	public record Item(long productId, String productSku, String productName, int quantity) {
	}
}
