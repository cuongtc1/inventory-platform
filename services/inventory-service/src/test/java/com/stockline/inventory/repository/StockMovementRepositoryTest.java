package com.stockline.inventory.repository;

import com.stockline.inventory.TestcontainersConfiguration;
import com.stockline.inventory.constant.MovementType;
import com.stockline.inventory.dto.StockMovementFilter;
import com.stockline.inventory.entity.StockLevel;
import com.stockline.inventory.entity.StockMovement;
import com.stockline.inventory.entity.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.stockline.inventory.TestData.warehouse;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The list filters on MySQL. {@code created_at} is set with plain SQL, because auditing always fills in "now". The
 * interesting case is the day boundary: Vietnam is UTC+7, so 22/10 starts at 2026-10-21T17:00:00Z.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class StockMovementRepositoryTest {

	@Autowired
	private StockMovementRepository stockMovementRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Warehouse main;
	private Warehouse north;

	@BeforeEach
	void setUp() {
		main = entityManager.persist(warehouse("WH-01", "Main"));
		north = entityManager.persist(warehouse("WH-02", "North"));
		movement("lastOf21", main, 1, "A01", "Wireless Mouse", MovementType.IN, "2026-10-21T16:59:59Z");
		movement("firstOf22", main, 2, "A02", "USB-C Cable", MovementType.ADJUST, "2026-10-21T17:00:00Z");
		movement("lastOf22", north, 1, "A01", "Wireless Mouse", MovementType.ADJUST, "2026-10-22T16:59:59Z");
		movement("firstOf23", north, 3, "B_1", "Desk 100% Oak", MovementType.OUT, "2026-10-22T17:00:00Z");
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void noFilters_returnsAll_newestFirst() {
		assertThat(find(filter(null, null, null, null, null, null)))
				.containsExactly("firstOf23", "lastOf22", "firstOf22", "lastOf21");
	}

	@Test
	void fromAndTo_coverWholeVietnamDays() {
		assertThat(find(filter(null, null, null, null, "2026-10-22", "2026-10-22")))
				.containsExactly("lastOf22", "firstOf22");
	}

	@Test
	void fromAlone_startsAtMidnightInVietnam() {
		assertThat(find(filter(null, null, null, null, "2026-10-23", null))).containsExactly("firstOf23");
	}

	@Test
	void toAlone_includesTheWholeDay() {
		assertThat(find(filter(null, null, null, null, null, "2026-10-21"))).containsExactly("lastOf21");
	}

	@Test
	void filter_byWarehouse() {
		assertThat(find(filter(null, main.getId(), null, null, null, null))).containsExactly("firstOf22", "lastOf21");
	}

	@Test
	void filter_byProduct() {
		assertThat(find(filter(null, null, 1L, null, null, null))).containsExactly("lastOf22", "lastOf21");
	}

	@Test
	void filter_byType() {
		assertThat(find(filter(null, null, null, MovementType.ADJUST, null, null)))
				.containsExactly("lastOf22", "firstOf22");
	}

	@Test
	void filter_searchInNameOrSku_withPlainTextWildcards() {
		assertThat(find(filter("CABLE", null, null, null, null, null))).containsExactly("firstOf22");
		assertThat(find(filter("a01", null, null, null, null, null))).containsExactly("lastOf22", "lastOf21");
		assertThat(find(filter("100%", null, null, null, null, null))).containsExactly("firstOf23");
		assertThat(find(filter("_", null, null, null, null, null))).containsExactly("firstOf23");
	}

	@Test
	void filters_combineWithAnd() {
		assertThat(find(filter("mouse", north.getId(), null, MovementType.ADJUST, "2026-10-22", "2026-10-22")))
				.containsExactly("lastOf22");
		assertThat(find(filter("mouse", main.getId(), null, MovementType.ADJUST, null, null))).isEmpty();
	}

	@Test
	void createdAt_isStoredInUtc_whateverTheZoneOfTheJvm() {
		// given: Hibernate maps an Instant as TIMESTAMP_UTC by default, so a laptop at UTC+7 still stores UTC
		StockMovement saved = stockMovementRepository.findAll().getFirst();

		// when
		LocalDateTime stored = jdbcTemplate.queryForObject(
				"SELECT created_at FROM stock_movements WHERE id = ?", LocalDateTime.class, saved.getId());

		// then
		assertThat(saved.getCreatedAt().atOffset(ZoneOffset.UTC).toLocalDateTime()).isEqualTo(stored);
	}

	/**
	 * Saves a movement whose reference is its name in the tests, then moves it to {@code createdAt}. The value goes
	 * in as a LocalDateTime on purpose: it is stored as written, while a java.sql.Timestamp is shifted to the zone of
	 * the JVM.
	 */
	private void movement(String name, Warehouse warehouse, long productId, String sku, String productName,
						  MovementType type, String createdAt) {
		StockLevel level = new StockLevel(warehouse, productId);
		level.copyProduct(sku, productName, 0);
		StockMovement movement = entityManager.persist(new StockMovement(level, type, 1, name));
		entityManager.flush();
		jdbcTemplate.update("UPDATE stock_movements SET created_at = ? WHERE id = ?",
				Instant.parse(createdAt).atOffset(ZoneOffset.UTC).toLocalDateTime(), movement.getId());
	}

	private static StockMovementFilter filter(String search, Long warehouseId, Long productId, MovementType type,
											  String from, String to) {
		return new StockMovementFilter(search, warehouseId, productId, type,
				from == null ? null : LocalDate.parse(from), to == null ? null : LocalDate.parse(to));
	}

	private List<String> find(StockMovementFilter filter) {
		return stockMovementRepository.findAll(StockMovementSpecifications.matching(filter),
						PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")))
				.map(StockMovement::getReference)
				.getContent();
	}
}
