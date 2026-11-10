package com.stockline.order.repository;

import com.stockline.common.util.TextUtils;
import com.stockline.order.constant.OrderStatus;
import com.stockline.order.dto.SalesOrderFilter;
import com.stockline.order.entity.SalesOrder;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the WHERE clause of the order list from the filters that were sent. Each filter is a small Specification;
 * {@code allOf} joins them with AND, and an empty list means "every order".
 */
public final class SalesOrderSpecifications {

	private SalesOrderSpecifications() {
	}

	public static Specification<SalesOrder> matching(SalesOrderFilter filter) {
		List<Specification<SalesOrder>> conditions = new ArrayList<>();
		if (filter.search() != null && !filter.search().isBlank()) {
			conditions.add(numberOrCustomerContains(filter.search().strip()));
		}
		if (filter.customerId() != null) {
			conditions.add(forCustomer(filter.customerId()));
		}
		if (filter.status() != null) {
			conditions.add(withStatus(filter.status()));
		}
		return Specification.allOf(conditions);
	}

	/** "1042" finds SO-1042; "acme" finds ACME Ltd's orders. */
	static Specification<SalesOrder> numberOrCustomerContains(String text) {
		String pattern = TextUtils.containsPattern(text);
		return (root, query, cb) -> cb.or(
				cb.like(cb.lower(root.get("orderNumber")), pattern, TextUtils.LIKE_ESCAPE),
				cb.like(cb.lower(root.get("customer").get("name")), pattern, TextUtils.LIKE_ESCAPE));
	}

	static Specification<SalesOrder> forCustomer(Long customerId) {
		return (root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId);
	}

	static Specification<SalesOrder> withStatus(OrderStatus status) {
		return (root, query, cb) -> cb.equal(root.get("status"), status);
	}
}
