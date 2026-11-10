package com.stockline.order.repository;

import com.stockline.order.entity.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {

	/** Redeclared only to add the entity graph: the customer comes in the same query, not one extra per row. */
	@Override
	@EntityGraph(attributePaths = "customer")
	Page<SalesOrder> findAll(Specification<SalesOrder> spec, Pageable pageable);

	/** One order with its customer and lines in one query, for the detail. ("WithItems" is only a name.) */
	@EntityGraph(attributePaths = {"customer", "items"})
	Optional<SalesOrder> findWithItemsById(Long id);

	/** Orders per customer, in one query for a whole page. Customers without orders have no row. */
	@Query("""
			SELECT o.customer.id AS customerId, COUNT(o) AS orderCount
			FROM SalesOrder o
			WHERE o.customer.id IN :customerIds
			GROUP BY o.customer.id
			""")
	List<CustomerOrderCount> countByCustomerIds(@Param("customerIds") Collection<Long> customerIds);

	/** One row of {@link #countByCustomerIds}: Spring Data fills in the getters from the aliases. */
	interface CustomerOrderCount {

		long getCustomerId();

		long getOrderCount();
	}
}
