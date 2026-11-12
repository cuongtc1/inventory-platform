package com.stockline.inventory.event;

import com.stockline.common.event.EventCodec;
import com.stockline.common.event.EventPublisher;
import com.stockline.common.event.KafkaTopics;
import com.stockline.inventory.constant.MessageKeys;
import com.stockline.inventory.service.StockReserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * The inventory side of the order saga: a new order arrives, its stock is reserved (or not), and order-service hears
 * the answer as {@code stock.reserved} or {@code stock.failed}, keyed by the order id.
 *
 * <p>The answer is sent after the reservation's transaction has committed. If sending fails, the error is only
 * logged: throwing would make Kafka deliver the order again and reserve its stock a second time. The order then stays
 * PENDING; the outbox (T6.5) closes that gap, and idempotency (T6.4) makes redelivery safe.
 */
@Component
public class OrderEventsListener {

	private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

	private final StockReserver stockReserver;
	private final EventPublisher eventPublisher;
	private final EventCodec eventCodec;
	private final MessageSource messageSource;

	public OrderEventsListener(StockReserver stockReserver, EventPublisher eventPublisher, EventCodec eventCodec,
							   MessageSource messageSource) {
		this.stockReserver = stockReserver;
		this.eventPublisher = eventPublisher;
		this.eventCodec = eventCodec;
		this.messageSource = messageSource;
	}

	@KafkaListener(topics = KafkaTopics.ORDER_CREATED)
	public void onOrderCreated(String json) {
		OrderCreatedEvent order = eventCodec.read(json, OrderCreatedEvent.class).payload();

		// A switch over a sealed type (Java 21): record patterns take the fields apart, and no default is needed
		switch (stockReserver.reserve(order)) {
			case StockReserver.Reserved(long orderId) ->
					send(KafkaTopics.STOCK_RESERVED, orderId, new StockReservedEvent(orderId), order.orderNumber());
			case StockReserver.Failed(long orderId, String reason) ->
					send(KafkaTopics.STOCK_FAILED, orderId, new StockFailedEvent(orderId, reason), order.orderNumber());
		}
	}

	private void send(String topic, long orderId, Object payload, String orderNumber) {
		eventPublisher.publish(topic, String.valueOf(orderId), payload).whenComplete((result, error) -> {
			if (error != null) {
				log.error(messageSource.getMessage(MessageKeys.STOCK_RESERVATION_PUBLISH_FAILED,
						new Object[] {topic, orderNumber}, Locale.ROOT), error);
			}
		});
	}
}
