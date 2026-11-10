package com.stockline.order.entity;

import com.stockline.common.exception.InvalidStatusTransitionException;
import com.stockline.order.constant.MessageKeys;
import com.stockline.order.constant.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.stockline.order.TestData.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The entity's own rules, without Spring or a database.
 */
class SalesOrderTest {

	@Test
	void newOrder_isPendingWithTheSumOfItsLines() {
		// when
		SalesOrder order = order();

		// then
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
		assertThat(order.getTotalAmount()).isEqualByComparingTo("108.00");
	}

	@Test
	void changeStatus_allowed_movesOnAndReturnsThePreviousStatus() {
		// given
		SalesOrder order = order();

		// when
		OrderStatus previous = order.changeStatus(OrderStatus.RESERVED);

		// then
		assertThat(previous).isEqualTo(OrderStatus.PENDING);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.RESERVED);
	}

	@Test
	void changeStatus_notAllowed_throwsAndChangesNothing() {
		// given
		SalesOrder order = order();

		// when / then: shipping an order that isn't paid
		assertThatThrownBy(() -> order.changeStatus(OrderStatus.SHIPPED))
				.isInstanceOfSatisfying(InvalidStatusTransitionException.class, ex -> {
					assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.ORDER_STATUS_CHANGE_NOT_ALLOWED);
					assertThat(ex.getMessageArgs()).containsExactly("SO-1001", OrderStatus.PENDING, OrderStatus.SHIPPED);
				});
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
	}

	@Test
	void theHappyPath_endsCompleted() {
		// given
		SalesOrder order = order();

		// when
		order.changeStatus(OrderStatus.RESERVED);
		order.changeStatus(OrderStatus.PAID);
		order.changeStatus(OrderStatus.SHIPPED);
		order.changeStatus(OrderStatus.COMPLETED);

		// then: nothing after that, not even a cancel
		assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
		assertThatThrownBy(() -> order.changeStatus(OrderStatus.CANCELLED))
				.isInstanceOf(InvalidStatusTransitionException.class);
	}

	/** The mockup's order: 5 × $12.00 + 2 × $24.00. */
	private static SalesOrder order() {
		SalesOrder order = new SalesOrder("SO-1001", customer("ACME Ltd"), 2L, "WH-01");
		order.addItem(6L, "A01", "Wireless Mouse", 5, new BigDecimal("12.00"));
		order.addItem(7L, "A02", "Laptop Stand", 2, new BigDecimal("24.00"));
		return order;
	}
}
