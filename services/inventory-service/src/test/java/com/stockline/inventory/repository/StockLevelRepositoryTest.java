package com.stockline.inventory.repository;

import com.stockline.inventory.TestcontainersConfiguration;
import com.stockline.inventory.constant.MovementType;
import com.stockline.inventory.dto.StockLevelFilter;
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

import java.util.List;

import static com.stockline.inventory.TestData.warehouse;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entities against the tables, and the list filters, on MySQL.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class StockLevelRepositoryTest {

	@Autowired
	private StockLevelRepository stockLevelRepository;

	@Autowired
	private StockMovementRepository stockMovementRepository;

	@Autowired
	private TestEntityManager entityManager;

	private Warehouse main;
	private Warehouse north;

	@BeforeEach
	void setUp() {
		main = entityManager.persist(warehouse("WH-01", "Main"));
		north = entityManager.persist(warehouse("WH-02", "North"));
		level(main, 1, "A01", "Wireless Mouse", 20, 140, 20);     // 120 available: fine
		level(main, 2, "A02", "USB-C Cable", 20, 24, 4);          // 20 available: exactly at the level, low
		level(north, 1, "A01", "Wireless Mouse", 20, 9, 0);       // 9 available: low
		level(north, 3, "B_1", "Desk 100% Oak", 0, 5, 0);         // 5 available, level 0: fine
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void filter_lowStock_availableAtOrBelowReorderLevel() {
		assertThat(find(new StockLevelFilter(null, null, null, true))).containsExactly("A01@WH-02", "A02@WH-01");
	}

	@Test
	void filter_lowStockFalse_meansNoFilter() {
		assertThat(find(new StockLevelFilter(null, null, null, false))).hasSize(4);
	}

	@Test
	void filter_byWarehouseAndProduct() {
		assertThat(find(new StockLevelFilter(null, main.getId(), null, null))).containsExactly("A01@WH-01", "A02@WH-01");
		assertThat(find(new StockLevelFilter(null, null, 1L, null))).containsExactly("A01@WH-01", "A01@WH-02");
	}

	@Test
	void filter_searchInNameOrSkuIgnoringCase() {
		assertThat(find(new StockLevelFilter("mouse", main.getId(), null, null))).containsExactly("A01@WH-01");
		assertThat(find(new StockLevelFilter("a02", null, null, null))).containsExactly("A02@WH-01");
	}

	@Test
	void filter_percentAndUnderscoreArePlainText() {
		assertThat(find(new StockLevelFilter("100%", null, null, null))).containsExactly("B_1@WH-02");
		assertThat(find(new StockLevelFilter("_", null, null, null))).containsExactly("B_1@WH-02");
	}

	@Test
	void sort_byWarehouseCodeWorks() {
		var page = stockLevelRepository.findAll(StockLevelSpecifications.matching(new StockLevelFilter(null, null, null, null)),
				PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "warehouse.code")));

		assertThat(page.getContent().getFirst().getWarehouse().getCode()).isEqualTo("WH-02");
	}

	@Test
	void update_raisesTheVersion() {
		// given
		StockLevel level = stockLevelRepository.findByWarehouseIdAndProductId(main.getId(), 1L).orElseThrow();
		assertThat(level.getVersion()).isZero();

		// when
		level.setOnHand(141);
		stockLevelRepository.flush();

		// then
		assertThat(level.getVersion()).isEqualTo(1);
	}

	@Test
	void countByWarehouseIds_countsRowsPerWarehouse() {
		// when
		var counts = stockLevelRepository.countByWarehouseIds(List.of(main.getId(), north.getId()));

		// then
		assertThat(counts).extracting(StockLevelRepository.WarehouseSkuCount::getSkuCount).containsOnly(2L);
	}

	@Test
	void movement_storesEveryColumn() {
		// given
		StockLevel level = stockLevelRepository.findByWarehouseIdAndProductId(main.getId(), 1L).orElseThrow();

		// when
		StockMovement saved = stockMovementRepository.saveAndFlush(new StockMovement(level, MovementType.ADJUST, -2, "Count"));
		entityManager.clear();

		// then
		StockMovement found = stockMovementRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getMovementType()).isEqualTo(MovementType.ADJUST);
		assertThat(found.getQuantity()).isEqualTo(-2);
		assertThat(found.getReference()).isEqualTo("Count");
		assertThat(found.getProductName()).isEqualTo("Wireless Mouse");
		assertThat(found.getWarehouse().getId()).isEqualTo(main.getId());
	}

	private void level(Warehouse warehouse, long productId, String sku, String name, int reorderLevel, int onHand,
					   int reserved) {
		StockLevel level = new StockLevel(warehouse, productId);
		level.copyProduct(sku, name, reorderLevel);
		level.setOnHand(onHand);
		level.setReserved(reserved);
		entityManager.persist(level);
	}

	/** Matching rows as "sku@warehouse", sorted. */
	private List<String> find(StockLevelFilter filter) {
		return stockLevelRepository.findAll(StockLevelSpecifications.matching(filter), PageRequest.of(0, 20))
				.map(level -> level.getProductSku() + "@" + level.getWarehouse().getCode())
				.getContent().stream().sorted().toList();
	}
}
