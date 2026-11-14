package com.stockline.inventory.event;

import com.stockline.common.event.EventCodec;
import com.stockline.common.event.KafkaTopics;
import com.stockline.inventory.service.StockReserver;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The inventory side of the order saga: a new order arrives and StockReserver reserves its stock, or not. The answer
 * for order-service (stock.reserved or stock.failed, keyed by the order id) is stored in the outbox in the same
 * transaction (T6.5), and the outbox publisher sends it. A redelivered order is recognized by its event id (T6.4) and
 * changes nothing.
 */
@Component
public class OrderEventsListener {

	private final StockReserver stockReserver;
	private final EventCodec eventCodec;

	public OrderEventsListener(StockReserver stockReserver, EventCodec eventCodec) {
		this.stockReserver = stockReserver;
		this.eventCodec = eventCodec;
	}

	@KafkaListener(topics = KafkaTopics.ORDER_CREATED)
	public void onOrderCreated(String json) {
		stockReserver.reserve(eventCodec.read(json, OrderCreatedEvent.class));
	}
}
