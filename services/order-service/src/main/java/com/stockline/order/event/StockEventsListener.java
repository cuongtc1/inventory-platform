package com.stockline.order.event;

import com.stockline.common.event.EventCodec;
import com.stockline.common.event.EventEnvelope;
import com.stockline.common.event.KafkaTopics;
import com.stockline.common.exception.InvalidStatusTransitionException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.order.constant.MessageKeys;
import com.stockline.order.constant.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * inventory-service's answer to {@code order.created}: the order moves to RESERVED or REJECTED, through the same
 * state machine as a user's action. There is no signed-in user on a listener thread, so the history row's author is
 * "system". StockEventHandler does the work in one transaction, at most once per event (T6.4).
 *
 * <p>An event that no longer fits is skipped, not retried: the order was cancelled meanwhile, or doesn't exist. A
 * retry wouldn't change that. Any other error goes back to Spring Kafka, which delivers the message again a few times
 * (e.g. after a version conflict with a user changing the same order).
 */
@Component
public class StockEventsListener {

	private static final Logger log = LoggerFactory.getLogger(StockEventsListener.class);

	private final StockEventHandler stockEventHandler;
	private final EventCodec eventCodec;
	private final MessageSource messageSource;

	public StockEventsListener(StockEventHandler stockEventHandler, EventCodec eventCodec, MessageSource messageSource) {
		this.stockEventHandler = stockEventHandler;
		this.eventCodec = eventCodec;
		this.messageSource = messageSource;
	}

	@KafkaListener(topics = KafkaTopics.STOCK_RESERVED)
	public void onStockReserved(String json) {
		EventEnvelope<StockReservedEvent> event = eventCodec.read(json, StockReservedEvent.class);
		skipIfNoLongerFits(event.payload().orderId(), OrderStatus.RESERVED, () -> stockEventHandler.stockReserved(event));
	}

	@KafkaListener(topics = KafkaTopics.STOCK_FAILED)
	public void onStockFailed(String json) {
		EventEnvelope<StockFailedEvent> event = eventCodec.read(json, StockFailedEvent.class);
		skipIfNoLongerFits(event.payload().orderId(), OrderStatus.REJECTED, () -> stockEventHandler.stockFailed(event));
	}

	private void skipIfNoLongerFits(long orderId, OrderStatus next, Runnable apply) {
		try {
			apply.run();
		} catch (InvalidStatusTransitionException | ResourceNotFoundException e) {
			log.warn(messageSource.getMessage(MessageKeys.ORDER_EVENT_SKIPPED, new Object[] {orderId, next,
					messageSource.getMessage(e.getMessageKey(), e.getMessageArgs(), Locale.ROOT)}, Locale.ROOT));
		}
	}
}
