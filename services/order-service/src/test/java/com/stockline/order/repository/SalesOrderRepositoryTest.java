package com.stockline.order.repository;

import com.stockline.order.TestcontainersConfiguration;
import com.stockline.order.constant.OrderStatus;
import com.stockline.order.dto.SalesOrderFilter;
import com.stockline.order.entity.Customer;
import com.stockline.order.entity.OrderStatusHistory;
import com.stockline.order.entity.SalesOrder;
import com.stockline.order.repository.SalesOrderRepository.CustomerOrderCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static com.stockline.order.TestData.customer;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The order entities against the tables, the list filters and the order count, on MySQL.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class SalesOrderRepositoryTest {

	@Autowired
	private SalesOrderRepository salesOrderRepository;

	@Autowired
	private OrderStatusHistoryRepository historyRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Customer acme;
	private Customer nova;

	@BeforeEach
	void setUp() {
		acme = entityManager.persist(customer("ACME Ltd"));
		nova = entityManager.persist(customer("Nova 100% Retail"));
		order("SO-1001", acme, "12.00", 5);
		order("SO-1002", nova, "24.00", 2);
		order("SO-1003", acme, "3.50", 1);
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void save_storesLinesWithTheOrder_andTheTotal() {
		// when
		SalesOrder loaded = salesOrderRepository.findWithItemsById(idOf("SO-1001")).orElseThrow();

		// then
		assertThat(loaded.getTotalAmount()).isEqualByComparingTo("60.00");
		assertThat(loaded.getStatus()).isEqualTo(OrderStatus.PENDING);
		assertThat(loaded.getCustomer().getName()).isEqualTo("ACME Ltd");
		assertThat(loaded.getItems()).singleElement().satisfies(item -> {
			assertThat(item.getProductName()).isEqualTo("Wireless Mouse");
			assertThat(item.getUnitPrice()).isEqualByComparingTo("12.00");
			assertThat(item.getLineTotal()).isEqualByComparingTo("60.00");
		});
		assertThat(jdbcTemplate.queryForObject("SELECT total_amount FROM sales_orders WHERE order_number = 'SO-1001'",
				BigDecimal.class)).isEqualByComparingTo("60.00");
	}

	@Test
	void history_isReadOldestFirst() {
		// given
		SalesOrder order = salesOrderRepository.findById(idOf("SO-1001")).orElseThrow();
		historyRepository.saveAndFlush(new OrderStatusHistory(order, null, OrderStatus.PENDING, null));
		historyRepository.saveAndFlush(new OrderStatusHistory(order, OrderStatus.PENDING, OrderStatus.REJECTED, "Short"));

		// when / then
		assertThat(historyRepository.findBySalesOrderIdOrderByCreatedAtAscIdAsc(order.getId()))
				.extracting(OrderStatusHistory::getToStatus)
				.containsExactly(OrderStatus.PENDING, OrderStatus.REJECTED);
	}

	@Test
	void filter_searchInNumberOrCustomerName() {
		assertThat(find(new SalesOrderFilter("1002", null, null))).containsExactly("SO-1002");
		assertThat(find(new SalesOrderFilter("acme", null, null))).containsExactly("SO-1003", "SO-1001");
		// % is plain text: it doesn't match every name
		assertThat(find(new SalesOrderFilter("100%", null, null))).containsExactly("SO-1002");
	}

	@Test
	void filter_byCustomerAndStatus() {
		assertThat(find(new SalesOrderFilter(null, nova.getId(), null))).containsExactly("SO-1002");
		assertThat(find(new SalesOrderFilter(null, null, OrderStatus.PENDING))).hasSize(3);
		assertThat(find(new SalesOrderFilter(null, null, OrderStatus.PAID))).isEmpty();
	}

	@Test
	void countByCustomerIds_countsPerCustomer_withoutRowsForCustomersWithoutOrders() {
		// given
		Customer delta = entityManager.persistFlushFind(customer("Delta Shop"));

		// when
		List<CustomerOrderCount> counts = salesOrderRepository.countByCustomerIds(
				List.of(acme.getId(), nova.getId(), delta.getId()));

		// then
		assertThat(counts).extracting(CustomerOrderCount::getCustomerId, CustomerOrderCount::getOrderCount)
				.containsExactlyInAnyOrder(
						org.assertj.core.groups.Tuple.tuple(acme.getId(), 2L),
						org.assertj.core.groups.Tuple.tuple(nova.getId(), 1L));
	}

	private void order(String number, Customer customer, String price, int quantity) {
		SalesOrder order = new SalesOrder(number, customer, 2L, "WH-01");
		order.addItem(6L, "A01", "Wireless Mouse", quantity, new BigDecimal(price));
		salesOrderRepository.save(order);
		// Different createdAt values for a stable "newest first"
		entityManager.flush();
		jdbcTemplate.update("UPDATE sales_orders SET created_at = TIMESTAMPADD(SECOND, ?, '2026-11-10 03:00:00') "
				+ "WHERE order_number = ?", Integer.parseInt(number.substring(3)), number);
	}

	private long idOf(String number) {
		return jdbcTemplate.queryForObject("SELECT id FROM sales_orders WHERE order_number = ?", Long.class, number);
	}

	private List<String> find(SalesOrderFilter filter) {
		return salesOrderRepository.findAll(SalesOrderSpecifications.matching(filter),
						PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")))
				.map(SalesOrder::getOrderNumber)
				.getContent();
	}
}
