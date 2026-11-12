package com.stockline.order.event;

/**
 * Payload of {@code stock.reserved}, as order-service reads it: inventory-service set aside the stock of every line of
 * the order. Other fields inventory may add are ignored.
 */
public record StockReservedEvent(long orderId) {
}
