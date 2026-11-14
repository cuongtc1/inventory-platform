package com.stockline.order.event;

import com.stockline.common.event.KafkaTopics;
import com.stockline.common.event.Outbox;
import com.stockline.order.entity.SalesOrder;
import org.springframework.stereotype.Component;

/**
 * The events order-service announces about an order. They go into the outbox (T6.5), in the transaction that changes
 * the order, and stockline-common's OutboxPublisher sends them to Kafka. So an order is never saved without its event,
 * and no event announces an order that was rolled back, even when Kafka is down at that moment.
 */
@Component
public class OrderEvents {

	private final Outbox outbox;

	public OrderEvents(Outbox outbox) {
		this.outbox = outbox;
	}

	/**
	 * {@code order.created}, with the order id as the key so all events of one order stay in order. Call it inside the
	 * transaction that saves the order, after the save, when the order has its id.
	 */
	public void orderCreated(SalesOrder order) {
		outbox.add(KafkaTopics.ORDER_CREATED, String.valueOf(order.getId()), new OrderCreatedEvent(order.getId(),
				order.getOrderNumber(), order.getWarehouseId(), order.getItems().stream()
						.map(item -> new OrderCreatedEvent.Item(item.getProductId(), item.getProductSku(),
								item.getProductName(), item.getQuantity()))
						.toList()));
	}
}
