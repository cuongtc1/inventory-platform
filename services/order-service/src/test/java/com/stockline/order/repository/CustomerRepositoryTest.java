package com.stockline.order.repository;

import com.stockline.order.TestcontainersConfiguration;
import com.stockline.order.entity.Customer;
import com.stockline.order.repository.CustomerRepository.CustomerOrderCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static com.stockline.order.TestData.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the entity against the table, the derived queries and the order count on MySQL.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class CustomerRepositoryTest {

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Customer acme;
	private Customer nova;

	@BeforeEach
	void setUp() {
		acme = customerRepository.saveAndFlush(new Customer("ACME Ltd", "buy@acme.com", "+84 28 3800 1122"));
		nova = customerRepository.saveAndFlush(customer("Nova Retail"));
		customerRepository.saveAndFlush(new Customer("Under_score", "ops@bluefox.vn", null));
	}

	@Test
	void search_matchesNameOrEmailIgnoringCase() {
		assertThat(search("acme")).containsExactly("ACME Ltd");
		assertThat(search("BLUEFOX")).containsExactly("Under_score");
	}

	@Test
	void search_emptyText_findsCustomersWithoutEmailToo() {
		assertThat(search("")).containsExactly("ACME Ltd", "Nova Retail", "Under_score");
	}

	@Test
	void search_underscoreIsPlainText() {
		assertThat(search("_")).containsExactly("Under_score");
	}

	@Test
	void existsByName_ignoresCase() {
		assertThat(customerRepository.existsByName("acme ltd")).isTrue();
		assertThat(customerRepository.existsByName("Delta Shop")).isFalse();
	}

	@Test
	void existsByNameAndIdNot_ownNameIsFree_otherNameIsTaken() {
		assertThat(customerRepository.existsByNameAndIdNot("ACME Ltd", acme.getId())).isFalse();
		assertThat(customerRepository.existsByNameAndIdNot("ACME Ltd", nova.getId())).isTrue();
	}

	@Test
	void save_sameNameInOtherCase_isRejectedByUniqueKey() {
		assertThatThrownBy(() -> customerRepository.saveAndFlush(customer("acme ltd")))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("uk_customers_name");
	}

	@Test
	void countOrdersByCustomerIds_countsPerCustomer_withoutRowsForCustomersWithoutOrders() {
		// given
		insertOrder("SO-1001", acme.getId());
		insertOrder("SO-1002", acme.getId());

		// when
		List<CustomerOrderCount> counts = customerRepository.countOrdersByCustomerIds(List.of(acme.getId(), nova.getId()));

		// then
		assertThat(counts).singleElement().satisfies(count -> {
			assertThat(count.getCustomerId()).isEqualTo(acme.getId());
			assertThat(count.getOrderCount()).isEqualTo(2);
		});
	}

	private List<String> search(String text) {
		return customerRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
						text, text, PageRequest.of(0, 20, Sort.by("name")))
				.map(Customer::getName).getContent();
	}

	private void insertOrder(String orderNumber, long customerId) {
		jdbcTemplate.update("""
				INSERT INTO sales_orders (order_number, customer_id, warehouse_id, warehouse_code, status, total_amount,
				                          created_at, updated_at, created_by, updated_by)
				VALUES (?, ?, 1, 'WH-01', 'PENDING', 60.00, NOW(6), NOW(6), 'test', 'test')
				""", orderNumber, customerId);
	}
}
