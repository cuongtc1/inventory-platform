package com.stockline.order.event;

import com.stockline.common.event.EventCodec;
import com.stockline.common.event.EventEnvelope;
import com.stockline.common.event.KafkaTopics;
import com.stockline.common.exception.InvalidStatusTransitionException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.order.constant.MessageKeys;
import com.stockline.order.constant.OrderStatus;
import com.stockline.order.service.OrderStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What order-service does with inventory's answer, without Kafka: the JSON goes straight into the listener methods.
 */
@ExtendWith(MockitoExtension.class)
class StockEventsListenerTest {

	private final EventCodec codec = new EventCodec(JsonMapper.builder().build());

	@Mock
	private OrderStatusService orderStatusService;

	private StockEventsListener listener;

	@BeforeEach
	void setUp() {
		ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
		messages.setBasenames("messages", "common-messages");
		messages.setDefaultEncoding("UTF-8");
		messages.setFallbackToSystemLocale(false);
		listener = new StockEventsListener(orderStatusService, codec, messages);
	}

	@Test
	void stockReserved_movesTheOrderToReserved() {
		listener.onStockReserved(json(KafkaTopics.STOCK_RESERVED, new StockReservedEvent(42)));

		verify(orderStatusService).changeStatus(42L, OrderStatus.RESERVED, null);
	}

	@Test
	void stockFailed_rejectsTheOrder_withTheReasonAsNote() {
		listener.onStockFailed(json(KafkaTopics.STOCK_FAILED,
				new StockFailedEvent(42, "Not enough stock of 'USB-C Cable' in WH-01: only 4 available")));

		verify(orderStatusService).changeStatus(42L, OrderStatus.REJECTED,
				"Not enough stock of 'USB-C Cable' in WH-01: only 4 available");
	}

	@Test
	void orderCancelledMeanwhile_isSkipped_notRetried() {
		// given: a manager cancelled the PENDING order before the reservation arrived
		when(orderStatusService.changeStatus(42L, OrderStatus.RESERVED, null)).thenThrow(new InvalidStatusTransitionException(
				MessageKeys.ORDER_STATUS_CHANGE_NOT_ALLOWED, "SO-1042", OrderStatus.CANCELLED, OrderStatus.RESERVED));

		// when / then: no exception, so Spring Kafka doesn't deliver it again
		assertThatCode(() -> listener.onStockReserved(json(KafkaTopics.STOCK_RESERVED, new StockReservedEvent(42))))
				.doesNotThrowAnyException();
	}

	@Test
	void unknownOrder_isSkipped() {
		// given
		when(orderStatusService.changeStatus(99L, OrderStatus.RESERVED, null))
				.thenThrow(new ResourceNotFoundException("SalesOrder", 99L));

		// when / then
		assertThatCode(() -> listener.onStockReserved(json(KafkaTopics.STOCK_RESERVED, new StockReservedEvent(99))))
				.doesNotThrowAnyException();
	}

	private String json(String topic, Object payload) {
		return codec.write(EventEnvelope.of(topic, payload, Clock.systemUTC()));
	}
}
