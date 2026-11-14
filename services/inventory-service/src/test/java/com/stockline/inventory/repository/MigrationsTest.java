package com.stockline.inventory.repository;

import com.stockline.inventory.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the Flyway migrations on real MySQL and checks the rules the tables promise. There are no entities yet
 * (T4.2 and T4.3 add them), so the rows are written with plain SQL. Each test rolls back.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class MigrationsTest {

	private static final String AUDIT_COLUMNS = "created_at, updated_at, created_by, updated_by";
	private static final String AUDIT_VALUES = "NOW(6), NOW(6), 'test', 'test'";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private long warehouseId;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("INSERT INTO warehouses (code, name, " + AUDIT_COLUMNS + ") VALUES ('WH-01', 'Main', "
				+ AUDIT_VALUES + ")");
		warehouseId = jdbcTemplate.queryForObject("SELECT id FROM warehouses WHERE code = 'WH-01'", Long.class);
	}

	@Test
	void allTablesExist() {
		assertThat(jdbcTemplate.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()", String.class))
				.contains("warehouses", "stock_levels", "stock_movements", "stock_reservations", "processed_events", "outbox_events");
	}

	@Test
	void stockLevel_startsAtVersionZeroWithNothingInStock() {
		// when
		insertStockLevel(warehouseId, 12, 0, 0);

		// then
		assertThat(jdbcTemplate.queryForMap("SELECT on_hand, reserved, reorder_level, version FROM stock_levels"))
				.containsEntry("on_hand", 0)
				.containsEntry("reserved", 0)
				.containsEntry("reorder_level", 0)
				.containsEntry("version", 0L);
	}

	@Test
	void warehouse_codeIsUnique() {
		assertRejected(() -> jdbcTemplate.update("INSERT INTO warehouses (code, name, " + AUDIT_COLUMNS
				+ ") VALUES ('WH-01', 'Copy', " + AUDIT_VALUES + ")"), "uk_warehouses_code");
	}

	@Test
	void stockLevel_oneRowPerProductAndWarehouse() {
		// given
		insertStockLevel(warehouseId, 12, 5, 0);

		// when / then
		assertRejected(() -> insertStockLevel(warehouseId, 12, 9, 0), "uk_stock_levels_warehouse_product");
	}

	@Test
	void stockLevel_needsAnExistingWarehouse() {
		assertRejected(() -> insertStockLevel(warehouseId + 1000, 12, 5, 0), "fk_stock_levels_warehouses");
	}

	@Test
	void stockLevel_productIdHasNoForeignKey() {
		// when: product 999999 doesn't exist here; it lives in another service's schema
		insertStockLevel(warehouseId, 999_999, 5, 0);

		// then
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stock_levels", Long.class)).isEqualTo(1);
	}

	@Test
	void stockLevel_onHandCantGoNegative() {
		assertRejected(() -> insertStockLevel(warehouseId, 12, -1, 0), "ck_stock_levels_on_hand");
	}

	@Test
	void stockLevel_cantReserveMoreThanOnHand() {
		// given
		insertStockLevel(warehouseId, 12, 5, 5);

		// when / then
		assertRejected(() -> jdbcTemplate.update("UPDATE stock_levels SET reserved = 6"), "ck_stock_levels_reserved");
	}

	@Test
	void warehouseWithStock_cantBeDeleted() {
		// given
		insertStockLevel(warehouseId, 12, 5, 0);

		// when / then
		assertRejected(() -> jdbcTemplate.update("DELETE FROM warehouses WHERE id = ?", warehouseId),
				"fk_stock_levels_warehouses");
	}

	@Test
	void movement_acceptsANegativeCorrection() {
		// when
		jdbcTemplate.update("INSERT INTO stock_movements (warehouse_id, product_id, product_sku, product_name, "
				+ "movement_type, quantity, reference, " + AUDIT_COLUMNS + ") VALUES (?, 12, 'A01', 'Mouse', 'ADJUST', "
				+ "-2, 'Count', " + AUDIT_VALUES + ")", warehouseId);

		// then
		assertThat(jdbcTemplate.queryForObject("SELECT quantity FROM stock_movements", Integer.class)).isEqualTo(-2);
	}

	@Test
	void reservation_oneRowPerOrderLine() {
		// given
		insertReservation(42, 12, 2);

		// when / then
		assertRejected(() -> insertReservation(42, 12, 3), "uk_stock_reservations_order_warehouse_product");
	}

	@Test
	void reservation_quantityMustBePositive() {
		assertRejected(() -> insertReservation(42, 12, 0), "ck_stock_reservations_quantity");
	}

	@Test
	void processedEvent_sameEventTwiceIsRejected() {
		// given
		String eventId = "6f1c2a1e-6a43-4c3e-9b8a-0d6f4b1e2c55";
		insertProcessedEvent(eventId);

		// when / then
		assertRejected(() -> insertProcessedEvent(eventId), "uk_processed_events_event_id");
	}

	private void insertStockLevel(long warehouse, long productId, int onHand, int reserved) {
		jdbcTemplate.update("INSERT INTO stock_levels (warehouse_id, product_id, product_sku, product_name, on_hand, "
				+ "reserved, " + AUDIT_COLUMNS + ") VALUES (?, ?, 'A01', 'Mouse', ?, ?, " + AUDIT_VALUES + ")",
				warehouse, productId, onHand, reserved);
	}

	private void insertReservation(long orderId, long productId, int quantity) {
		jdbcTemplate.update("INSERT INTO stock_reservations (order_id, warehouse_id, product_id, quantity, status, "
				+ AUDIT_COLUMNS + ") VALUES (?, ?, ?, ?, 'RESERVED', " + AUDIT_VALUES + ")",
				orderId, warehouseId, productId, quantity);
	}

	private void insertProcessedEvent(String eventId) {
		jdbcTemplate.update("INSERT INTO processed_events (event_id, event_type, " + AUDIT_COLUMNS + ") VALUES (?, "
				+ "'order.created', " + AUDIT_VALUES + ")", eventId);
	}

	// DataAccessException: Spring reports a unique or foreign key as DataIntegrityViolationException, but has no
	// category for MySQL's CHECK error (3819), which arrives as UncategorizedSQLException
	private static void assertRejected(Runnable change, String constraint) {
		assertThatThrownBy(change::run)
				.isInstanceOf(DataAccessException.class)
				.hasMessageContaining(constraint);
	}
}
