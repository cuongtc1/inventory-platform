package com.stockline.order.event;

import com.stockline.common.event.EventPublisher;
import com.stockline.common.event.KafkaTopics;
import com.stockline.order.constant.MessageKeys;
import com.stockline.order.entity.SalesOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Tells the other services what happened to an order, through Kafka.
 *
 * <p>Called after the order's transaction has committed, so no event announces an order that was rolled back. The
 * send is asynchronous and doesn't hold up the HTTP request; a failure is only logged. That means an order can stay
 * PENDING if Kafka is down at that moment: the outbox (T6.5) closes that gap by storing the event with the order.
 */
@Component
public class OrderEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

	private final EventPublisher eventPublisher;
	private final MessageSource messageSource;

	public OrderEventPublisher(EventPublisher eventPublisher, MessageSource messageSource) {
		this.eventPublisher = eventPublisher;
		this.messageSource = messageSource;
	}

	/** {@code order.created}, with the order id as the key so all events of one order stay in order. */
	public void orderCreated(SalesOrder order) {
		OrderCreatedEvent event = new OrderCreatedEvent(order.getId(), order.getOrderNumber(), order.getWarehouseId(),
				order.getItems().stream()
						.map(item -> new OrderCreatedEvent.Item(item.getProductId(), item.getProductSku(),
								item.getProductName(), item.getQuantity()))
						.toList());
		eventPublisher.publish(KafkaTopics.ORDER_CREATED, String.valueOf(order.getId()), event)
				.whenComplete((result, error) -> {
					if (error != null) {
						log.error(messageSource.getMessage(MessageKeys.ORDER_EVENT_PUBLISH_FAILED,
								new Object[] {KafkaTopics.ORDER_CREATED, order.getOrderNumber()}, Locale.ROOT), error);
					}
				});
	}
}
