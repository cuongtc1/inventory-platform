package com.stockline.order.repository;

import com.stockline.order.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	/** Part of the name or the email, ignoring case; Spring Data escapes % and _ in the text. */
	Page<Customer> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);

	// Case-insensitive through MySQL's default collation, like the unique key uk_customers_name
	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, Long id);

	/**
	 * Orders per customer, in one query for a whole page. Customers without orders have no row. Native SQL because
	 * there is no SalesOrder entity yet; T5.3 adds it, and this can become JPQL then.
	 */
	@Query(value = """
			SELECT customer_id AS customerId, COUNT(*) AS orderCount
			FROM sales_orders
			WHERE customer_id IN (:customerIds)
			GROUP BY customer_id
			""", nativeQuery = true)
	List<CustomerOrderCount> countOrdersByCustomerIds(@Param("customerIds") Collection<Long> customerIds);

	/** One row of {@link #countOrdersByCustomerIds}: Spring Data fills in the getters from the column aliases. */
	interface CustomerOrderCount {

		long getCustomerId();

		long getOrderCount();
	}
}
