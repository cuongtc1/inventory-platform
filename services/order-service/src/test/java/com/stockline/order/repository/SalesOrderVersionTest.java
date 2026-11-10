package com.stockline.order.repository;

import com.stockline.order.TestcontainersConfiguration;
import com.stockline.order.constant.OrderStatus;
import com.stockline.order.entity.Customer;
import com.stockline.order.entity.SalesOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static com.stockline.order.TestData.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code @Version} on real MySQL: two changes to the same order, where the second was read before the first was saved.
 * Without the version, the second would silently overwrite the first (a "lost update"). The library turns this
 * exception into 409 CONCURRENT_UPDATE. NOT_SUPPORTED: the two changes need transactions of their own that commit.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SalesOrderVersionTest {

	@Autowired
	private SalesOrderRepository salesOrderRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	void aChangeBasedOnAnOldReading_isRefused() {
		// given
		Customer buyer = customerRepository.save(customer("Version Test Ltd"));
		SalesOrder order = new SalesOrder("SO-V001", buyer, 2L, "WH-01");
		order.addItem(6L, "A01", "Wireless Mouse", 1, new BigDecimal("12.00"));
		long id = salesOrderRepository.save(order).getId();

		TransactionTemplate outer = new TransactionTemplate(transactionManager);
		TransactionTemplate other = new TransactionTemplate(transactionManager);
		other.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		// when / then
		assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
			// A user reads the order (version 0) ...
			SalesOrder read = salesOrderRepository.findById(id).orElseThrow();
			// ... meanwhile someone else changes it and commits (version 1) ...
			other.executeWithoutResult(s -> jdbcTemplate.update(
					"UPDATE sales_orders SET status = 'CANCELLED', version = version + 1 WHERE id = ?", id));
			// ... and the user's change, based on version 0, is refused when it is written
			read.changeStatus(OrderStatus.RESERVED);
			salesOrderRepository.flush();
		})).isInstanceOf(ObjectOptimisticLockingFailureException.class);

		// The other change stays
		assertThat(jdbcTemplate.queryForObject("SELECT status FROM sales_orders WHERE id = ?", String.class, id))
				.isEqualTo("CANCELLED");
	}
}
