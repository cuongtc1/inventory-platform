package com.stockline.order;

import com.stockline.order.entity.Customer;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Ready-made entities for tests. Use with a static import:
 * {@code import static com.stockline.order.TestData.customer;}
 *
 * <p>Methods with an {@code id} are for unit tests, where no database generates ids.
 */
public final class TestData {

	private TestData() {
	}

	public static Customer customer(String name) {
		return new Customer(name, null, null);
	}

	public static Customer customer(Long id, String name, String email, String phone) {
		Customer customer = new Customer(name, email, phone);
		// The id is set by the database; unit tests have none, so set the private field directly
		ReflectionTestUtils.setField(customer, "id", id);
		return customer;
	}
}
