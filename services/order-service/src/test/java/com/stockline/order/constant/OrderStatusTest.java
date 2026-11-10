package com.stockline.order.constant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;
import java.util.Set;

import static com.stockline.order.constant.OrderStatus.CANCELLED;
import static com.stockline.order.constant.OrderStatus.COMPLETED;
import static com.stockline.order.constant.OrderStatus.PAID;
import static com.stockline.order.constant.OrderStatus.PENDING;
import static com.stockline.order.constant.OrderStatus.REJECTED;
import static com.stockline.order.constant.OrderStatus.RESERVED;
import static com.stockline.order.constant.OrderStatus.SHIPPED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole state machine as a table: for every status, the statuses it may change to. Every other pair (all 49 are
 * checked) must be refused.
 */
class OrderStatusTest {

	private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
			PENDING, Set.of(RESERVED, REJECTED, CANCELLED),
			RESERVED, Set.of(PAID, CANCELLED),
			PAID, Set.of(SHIPPED, CANCELLED),
			SHIPPED, Set.of(COMPLETED),
			COMPLETED, Set.of(),
			REJECTED, Set.of(),
			CANCELLED, Set.of());

	@ParameterizedTest
	@EnumSource(OrderStatus.class)
	void everyPair_isAllowedOnlyIfInTheTable(OrderStatus from) {
		for (OrderStatus to : OrderStatus.values()) {
			assertThat(from.canChangeTo(to))
					.as("%s → %s", from, to)
					.isEqualTo(ALLOWED.get(from).contains(to));
		}
	}

	@Test
	void theTable_coversEveryStatus() {
		// A new status in the enum must be added to the table above too
		assertThat(ALLOWED).containsOnlyKeys(OrderStatus.values());
	}

	@Test
	void cancelling_isOnlyPossibleBeforeShipping() {
		assertThat(OrderStatus.values())
				.filteredOn(status -> status.canChangeTo(CANCELLED))
				.containsExactlyInAnyOrder(PENDING, RESERVED, PAID);
	}

	@Test
	void finalStatuses() {
		assertThat(OrderStatus.values())
				.filteredOn(OrderStatus::isFinal)
				.containsExactlyInAnyOrder(COMPLETED, REJECTED, CANCELLED);
	}

	@Test
	void noStatus_changesToItself() {
		assertThat(OrderStatus.values()).noneMatch(status -> status.canChangeTo(status));
	}
}
