package com.stockline.inventory.event;

/**
 * Payload of {@code stock.failed}: the order couldn't get its stock, and nothing was reserved. Key: the order id.
 *
 * @param reason for people, e.g. "Not enough stock of 'USB-C Cable' in WH-01: only 4 available"
 */
public record StockFailedEvent(long orderId, String reason) {
}
