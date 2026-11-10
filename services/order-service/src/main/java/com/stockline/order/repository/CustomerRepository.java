package com.stockline.order.repository;

import com.stockline.order.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	/** Part of the name or the email, ignoring case; Spring Data escapes % and _ in the text. */
	Page<Customer> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);

	// Case-insensitive through MySQL's default collation, like the unique key uk_customers_name
	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, Long id);
}
