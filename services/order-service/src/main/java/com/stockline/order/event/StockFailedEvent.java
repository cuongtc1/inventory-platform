package com.stockline.order.event;

/**
 * Payload of {@code stock.failed}, as order-service reads it: the order couldn't get its stock. {@code reason} is a
 * text for people, e.g. "Not enough stock of 'USB-C Cable' in WH-01: only 4 available"; it becomes the note of the
 * REJECTED row in the order's history.
 */
public record StockFailedEvent(long orderId, String reason) {
}
