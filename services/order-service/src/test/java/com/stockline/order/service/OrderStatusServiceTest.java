package com.stockline.order.service;

import com.stockline.common.exception.InvalidStatusTransitionException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.order.constant.OrderStatus;
import com.stockline.order.dto.SalesOrderResponse;
import com.stockline.order.entity.OrderStatusHistory;
import com.stockline.order.entity.SalesOrder;
import com.stockline.order.mapper.SalesOrderMapper;
import com.stockline.order.repository.OrderStatusHistoryRepository;
import com.stockline.order.repository.SalesOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.stockline.order.TestData.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatusServiceTest {

	@Mock
	private SalesOrderRepository salesOrderRepository;

	@Mock
	private OrderStatusHistoryRepository historyRepository;

	private OrderStatusService service;

	@BeforeEach
	void setUp() {
		service = new OrderStatusService(salesOrderRepository, historyRepository, new SalesOrderMapper());
	}

	@Test
	void changeStatus_allowed_recordsFromAndToWithTheNote_andFlushes() {
		// given
		when(salesOrderRepository.findWithItemsById(5L)).thenReturn(Optional.of(order()));

		// when
		SalesOrderResponse response = service.changeStatus(5L, OrderStatus.REJECTED, "Not enough A01");

		// then
		assertThat(response.status()).isEqualTo(OrderStatus.REJECTED);
		ArgumentCaptor<OrderStatusHistory> history = ArgumentCaptor.forClass(OrderStatusHistory.class);
		verify(historyRepository).save(history.capture());
		assertThat(history.getValue().getFromStatus()).isEqualTo(OrderStatus.PENDING);
		assertThat(history.getValue().getToStatus()).isEqualTo(OrderStatus.REJECTED);
		assertThat(history.getValue().getNote()).isEqualTo("Not enough A01");
		verify(salesOrderRepository).flush();
	}

	@Test
	void changeStatus_notAllowed_recordsNothing() {
		// given
		when(salesOrderRepository.findWithItemsById(5L)).thenReturn(Optional.of(order()));

		// when / then
		assertThatThrownBy(() -> service.changeStatus(5L, OrderStatus.PAID, null))
				.isInstanceOf(InvalidStatusTransitionException.class);
		verify(historyRepository, never()).save(any());
	}

	@Test
	void changeStatus_unknownOrder_throwsNotFound() {
		// given
		when(salesOrderRepository.findWithItemsById(9L)).thenReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> service.changeStatus(9L, OrderStatus.CANCELLED, null))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void history_unknownOrder_throwsNotFound() {
		// given
		when(salesOrderRepository.existsById(9L)).thenReturn(false);

		// when / then
		assertThatThrownBy(() -> service.history(9L)).isInstanceOf(ResourceNotFoundException.class);
	}

	private static SalesOrder order() {
		SalesOrder order = new SalesOrder("SO-1001", customer(1L, "ACME Ltd", null, null), 2L, "WH-01");
		order.addItem(6L, "A01", "Wireless Mouse", 5, new BigDecimal("12.00"));
		return order;
	}
}
