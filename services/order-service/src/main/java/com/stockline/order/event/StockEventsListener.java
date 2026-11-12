package com.stockline.order.event;

import com.stockline.common.event.EventCodec;
import com.stockline.common.event.KafkaTopics;
import com.stockline.common.exception.InvalidStatusTransitionException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.order.constant.MessageKeys;
import com.stockline.order.constant.OrderStatus;
import com.stockline.order.service.OrderStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * inventory-service's answer to {@code order.created}: the order moves to RESERVED or REJECTED, through the same
 * state machine as a user's action (OrderStatusService). There is no signed-in user on a listener thread, so the
 * history row's author is "system".
 *
 * <p>An event that no longer fits is skipped, not retried: the order was cancelled meanwhile, or doesn't exist. A
 * retry wouldn't change that. Any other error goes back to Spring Kafka, which delivers the message again a few times
 * (e.g. after a version conflict with a user changing the same order).
 */
@Component
public class StockEventsListener {

	private static final Logger log = LoggerFactory.getLogger(StockEventsListener.class);

	private final OrderStatusService orderStatusService;
	private final EventCodec eventCodec;
	private final MessageSource messageSource;

	public StockEventsListener(OrderStatusService orderStatusService, EventCodec eventCodec,
							   MessageSource messageSource) {
		this.orderStatusService = orderStatusService;
		this.eventCodec = eventCodec;
		this.messageSource = messageSource;
	}

	@KafkaListener(topics = KafkaTopics.STOCK_RESERVED)
	public void onStockReserved(String json) {
		StockReservedEvent event = eventCodec.read(json, StockReservedEvent.class).payload();
		changeStatus(event.orderId(), OrderStatus.RESERVED, null);
	}

	@KafkaListener(topics = KafkaTopics.STOCK_FAILED)
	public void onStockFailed(String json) {
		StockFailedEvent event = eventCodec.read(json, StockFailedEvent.class).payload();
		changeStatus(event.orderId(), OrderStatus.REJECTED, event.reason());
	}

	private void changeStatus(long orderId, OrderStatus next, String note) {
		try {
			orderStatusService.changeStatus(orderId, next, note);
		} catch (InvalidStatusTransitionException | ResourceNotFoundException e) {
			log.warn(messageSource.getMessage(MessageKeys.ORDER_EVENT_SKIPPED, new Object[] {orderId, next,
					messageSource.getMessage(e.getMessageKey(), e.getMessageArgs(), Locale.ROOT)}, Locale.ROOT));
		}
	}
}
