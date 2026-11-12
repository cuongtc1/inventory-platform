package com.stockline.inventory.event;

/**
 * Payload of {@code stock.reserved}: the stock of every line of the order is set aside. Key: the order id.
 */
public record StockReservedEvent(long orderId) {
}
