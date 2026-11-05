package com.stockline.inventory.repository;

import com.stockline.inventory.TestcontainersConfiguration;
import com.stockline.inventory.entity.Warehouse;
import com.stockline.inventory.repository.StockLevelRepository.WarehouseSkuCount;
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

import static com.stockline.inventory.TestData.warehouse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the entity against the table, the derived queries and the SKU count on MySQL.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class WarehouseRepositoryTest {

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Autowired
	private StockLevelRepository stockLevelRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Warehouse main;
	private Warehouse north;

	@BeforeEach
	void setUp() {
		main = warehouseRepository.saveAndFlush(new Warehouse("WH-01", "Main Warehouse", "District 7"));
		north = warehouseRepository.saveAndFlush(warehouse("WH-02", "North Hub"));
		warehouseRepository.saveAndFlush(warehouse("DC_1", "Depot"));
	}

	@Test
	void search_matchesCodeOrNameIgnoringCase() {
		assertThat(search("wh-")).containsExactly("WH-01", "WH-02");
		assertThat(search("HUB")).containsExactly("WH-02");
	}

	@Test
	void search_underscoreIsPlainText() {
		assertThat(search("_")).containsExactly("DC_1");
	}

	@Test
	void existsByCode_ignoresCase() {
		assertThat(warehouseRepository.existsByCode("wh-01")).isTrue();
		assertThat(warehouseRepository.existsByCode("WH-99")).isFalse();
	}

	@Test
	void existsByCodeAndIdNot_ownCodeIsFree_otherCodeIsTaken() {
		assertThat(warehouseRepository.existsByCodeAndIdNot("WH-01", main.getId())).isFalse();
		assertThat(warehouseRepository.existsByCodeAndIdNot("WH-01", north.getId())).isTrue();
	}

	@Test
	void save_sameCodeInOtherCase_isRejectedByUniqueKey() {
		assertThatThrownBy(() -> warehouseRepository.saveAndFlush(warehouse("wh-01", "Copy")))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("uk_warehouses_code");
	}

	@Test
	void countByWarehouseIds_countsStockLevelsPerWarehouse_withoutRowsForEmptyOnes() {
		// given
		insertStockLevel(main.getId(), 12);
		insertStockLevel(main.getId(), 13);

		// when
		List<WarehouseSkuCount> counts = stockLevelRepository.countByWarehouseIds(List.of(main.getId(), north.getId()));

		// then
		assertThat(counts).singleElement().satisfies(count -> {
			assertThat(count.getWarehouseId()).isEqualTo(main.getId());
			assertThat(count.getSkuCount()).isEqualTo(2);
		});
	}

	private List<String> search(String text) {
		return warehouseRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(
						text, text, PageRequest.of(0, 20, Sort.by("code")))
				.map(Warehouse::getCode).getContent();
	}

	private void insertStockLevel(long warehouseId, long productId) {
		jdbcTemplate.update("""
				INSERT INTO stock_levels (warehouse_id, product_id, product_sku, product_name,
				                          created_at, updated_at, created_by, updated_by)
				VALUES (?, ?, 'A01', 'Mouse', NOW(6), NOW(6), 'test', 'test')
				""", warehouseId, productId);
	}
}
