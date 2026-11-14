package com.stockline.order.event;

import com.stockline.common.event.EventEnvelope;
import com.stockline.common.event.ProcessedEvents;
import com.stockline.order.constant.OrderStatus;
import com.stockline.order.service.OrderStatusService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies inventory's answer to an order, at most once per event (T6.4): the event is claimed in
 * {@code processed_events} and the status changed in one transaction. OrderStatusService.changeStatus joins it.
 *
 * <p>If the change is refused (the order was cancelled meanwhile), the whole transaction rolls back, the claim
 * included; StockEventsListener, outside the transaction, logs and skips it. A redelivery then meets the same refusal.
 */
@Component
public class StockEventHandler {

	private final ProcessedEvents processedEvents;
	private final OrderStatusService orderStatusService;

	public StockEventHandler(ProcessedEvents processedEvents, OrderStatusService orderStatusService) {
		this.processedEvents = processedEvents;
		this.orderStatusService = orderStatusService;
	}

	@Transactional
	public void stockReserved(EventEnvelope<StockReservedEvent> event) {
		if (processedEvents.claim(event)) {
			orderStatusService.changeStatus(event.payload().orderId(), OrderStatus.RESERVED, null);
		}
	}

	@Transactional
	public void stockFailed(EventEnvelope<StockFailedEvent> event) {
		if (processedEvents.claim(event)) {
			orderStatusService.changeStatus(event.payload().orderId(), OrderStatus.REJECTED, event.payload().reason());
		}
	}
}
