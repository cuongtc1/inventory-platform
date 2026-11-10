package com.stockline.order.repository;

import com.stockline.order.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the Flyway migrations on real MySQL and checks the rules the tables promise. There are no entities yet
 * for every table), so the rows are written with plain SQL. Each test rolls back.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class MigrationsTest {

	private static final String AUDIT_COLUMNS = "created_at, updated_at, created_by, updated_by";
	private static final String AUDIT_VALUES = "NOW(6), NOW(6), 'test', 'test'";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private long customerId;

	@BeforeEach
	void setUp() {
		customerId = insertCustomer("ACME Ltd");
	}

	@Test
	void allTablesExist() {
		assertThat(jdbcTemplate.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()", String.class))
				.contains("customers", "sales_orders", "sales_order_items", "order_status_history", "number_sequences");
	}

	@Test
	void salesOrderNumbers_startAt1001() {
		assertThat(jdbcTemplate.queryForObject("SELECT next_value FROM number_sequences WHERE name = 'sales_order'",
				Long.class)).isEqualTo(1001L);
	}

	@Test
	void numberSequence_nameIsUnique() {
		assertRejected(() -> jdbcTemplate.update("INSERT INTO number_sequences (name, next_value, " + AUDIT_COLUMNS
				+ ") VALUES ('sales_order', 1, " + AUDIT_VALUES + ")"), "uk_number_sequences_name");
	}

	@Test
	void customer_nameIsUnique() {
		assertRejected(() -> insertCustomer("ACME Ltd"), "uk_customers_name");
	}

	@Test
	void customer_contactDetailsAreOptional() {
		assertThat(jdbcTemplate.queryForMap("SELECT email, phone FROM customers WHERE id = ?", customerId))
				.containsEntry("email", null)
				.containsEntry("phone", null);
	}

	@Test
	void customerWithOrders_cantBeDeleted() {
		// given
		insertOrder("SO-1001", customerId, "60.00");

		// when / then
		assertRejected(() -> jdbcTemplate.update("DELETE FROM customers WHERE id = ?", customerId),
				"fk_sales_orders_customers");
	}

	@Test
	void order_startsAtVersionZero_andKeepsTwoDecimals() {
		// when
		long orderId = insertOrder("SO-1001", customerId, "1234.50");

		// then
		assertThat(jdbcTemplate.queryForMap("SELECT version, total_amount FROM sales_orders WHERE id = ?", orderId))
				.containsEntry("version", 0L)
				.containsEntry("total_amount", new BigDecimal("1234.50"));
	}

	@Test
	void order_numberIsUnique() {
		// given
		insertOrder("SO-1001", customerId, "60.00");

		// when / then
		assertRejected(() -> insertOrder("SO-1001", customerId, "70.00"), "uk_sales_orders_order_number");
	}

	@Test
	void order_needsAnExistingCustomer() {
		assertRejected(() -> insertOrder("SO-1001", customerId + 1000, "60.00"), "fk_sales_orders_customers");
	}

	@Test
	void order_totalIsNeverNegative() {
		assertRejected(() -> insertOrder("SO-1001", customerId, "-0.01"), "ck_sales_orders_total_amount");
	}

	@Test
	void item_needsAnExistingOrder() {
		assertRejected(() -> insertItem(999_999, 12, 1, "12.00"), "fk_sales_order_items_sales_orders");
	}

	@Test
	void item_oneLinePerProduct() {
		// given
		long orderId = insertOrder("SO-1001", customerId, "60.00");
		insertItem(orderId, 12, 5, "12.00");

		// when / then
		assertRejected(() -> insertItem(orderId, 12, 1, "12.00"), "uk_sales_order_items_order_product");
	}

	@Test
	void item_sameProductOnTwoOrders_isFine() {
		// given
		insertItem(insertOrder("SO-1001", customerId, "60.00"), 12, 5, "12.00");

		// when
		insertItem(insertOrder("SO-1002", customerId, "12.00"), 12, 1, "12.00");

		// then
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sales_order_items", Long.class)).isEqualTo(2);
	}

	@Test
	void item_quantityIsAboveZero() {
		long orderId = insertOrder("SO-1001", customerId, "0.00");
		assertRejected(() -> insertItem(orderId, 12, 0, "12.00"), "ck_sales_order_items_quantity");
	}

	@Test
	void item_priceIsNeverNegative() {
		long orderId = insertOrder("SO-1001", customerId, "0.00");
		assertRejected(() -> insertItem(orderId, 12, 1, "-1.00"), "ck_sales_order_items_unit_price");
	}

	@Test
	void history_firstRowHasNoFromStatus() {
		// given
		long orderId = insertOrder("SO-1001", customerId, "60.00");

		// when
		insertHistory(orderId, null, "PENDING");

		// then
		assertThat(jdbcTemplate.queryForMap(
				"SELECT from_status, to_status, note FROM order_status_history WHERE sales_order_id = ?", orderId))
				.containsEntry("from_status", null)
				.containsEntry("to_status", "PENDING")
				.containsEntry("note", null);
	}

	@Test
	void history_needsAnExistingOrder() {
		assertRejected(() -> insertHistory(999_999, null, "PENDING"), "fk_order_status_history_sales_orders");
	}

	@Test
	void noForeignKeyLeavesThisSchema() {
		// product_id and warehouse_id live in other services' schemas; order_db has no foreign key to them
		assertThat(jdbcTemplate.queryForList("""
				SELECT DISTINCT referenced_table_schema FROM information_schema.key_column_usage
				WHERE table_schema = DATABASE() AND referenced_table_name IS NOT NULL
				""", String.class))
				.containsOnly(jdbcTemplate.queryForObject("SELECT DATABASE()", String.class));
		assertThat(jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM information_schema.key_column_usage
				WHERE table_schema = DATABASE() AND referenced_table_name IS NOT NULL
				AND column_name IN ('product_id', 'warehouse_id')
				""", Long.class)).isZero();
	}

	private long insertCustomer(String name) {
		jdbcTemplate.update("INSERT INTO customers (name, " + AUDIT_COLUMNS + ") VALUES (?, " + AUDIT_VALUES + ")", name);
		return lastId();
	}

	private long insertOrder(String orderNumber, long customer, String total) {
		jdbcTemplate.update("INSERT INTO sales_orders (order_number, customer_id, warehouse_id, warehouse_code, status, "
				+ "total_amount, " + AUDIT_COLUMNS + ") VALUES (?, ?, 1, 'WH-01', 'PENDING', ?, " + AUDIT_VALUES + ")",
				orderNumber, customer, new BigDecimal(total));
		return lastId();
	}

	private void insertItem(long orderId, long productId, int quantity, String unitPrice) {
		jdbcTemplate.update("INSERT INTO sales_order_items (sales_order_id, product_id, product_sku, product_name, "
				+ "quantity, unit_price, " + AUDIT_COLUMNS + ") VALUES (?, ?, 'A01', 'Wireless Mouse', ?, ?, "
				+ AUDIT_VALUES + ")", orderId, productId, quantity, new BigDecimal(unitPrice));
	}

	private void insertHistory(long orderId, String fromStatus, String toStatus) {
		jdbcTemplate.update("INSERT INTO order_status_history (sales_order_id, from_status, to_status, " + AUDIT_COLUMNS
				+ ") VALUES (?, ?, ?, " + AUDIT_VALUES + ")", orderId, fromStatus, toStatus);
	}

	private long lastId() {
		return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
	}

	/** Spring has no category for MySQL's CHECK error, so only the base class is expected, plus the constraint's name. */
	private static void assertRejected(Runnable statement, String constraint) {
		assertThatThrownBy(statement::run)
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining(constraint);
	}
}
