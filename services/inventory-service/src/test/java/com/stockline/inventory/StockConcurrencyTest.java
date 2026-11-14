package com.stockline.inventory;

import com.stockline.common.event.EventEnvelope;
import com.stockline.common.event.KafkaTopics;
import com.stockline.inventory.dto.AdjustStockRequest;
import com.stockline.inventory.dto.ProductSnapshot;
import com.stockline.inventory.entity.StockLevel;
import com.stockline.inventory.entity.Warehouse;
import com.stockline.inventory.event.OrderCreatedEvent;
import com.stockline.inventory.repository.StockLevelRepository;
import com.stockline.inventory.repository.WarehouseRepository;
import com.stockline.inventory.service.StockAdjuster;
import com.stockline.inventory.service.StockReserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.resilience.retry.MethodRetryEvent;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Optimistic locking with retry on real MySQL (T6.3): several threads change the same stock level at the same moment,
 * like listener threads or several instances of the service. A barrier releases them together, so they read the same
 * version and collide. No stock may be sold twice, and nobody may fail because of the collision itself.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, StockConcurrencyTest.RetryCounter.class})
class StockConcurrencyTest {

	/** Counts the retries Spring makes: each one publishes a MethodRetryEvent. */
	@TestConfiguration
	static class RetryCounter {

		final AtomicInteger retries = new AtomicInteger();

		@EventListener
		void onRetry(MethodRetryEvent event) {
			retries.incrementAndGet();
		}
	}

	@Autowired
	private StockReserver stockReserver;

	@Autowired
	private StockAdjuster stockAdjuster;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Autowired
	private StockLevelRepository stockLevelRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RetryCounter retryCounter;

	private Warehouse warehouse;

	@BeforeEach
	void setUp() {
		warehouse = warehouseRepository.save(new Warehouse("CC-" + System.nanoTime() % 100_000, "Concurrency test", null));
	}

	@Test
	void twoOrdersForTheLastItems_onlyOneGetsThem() throws Exception {
		// given: 5 left, two orders want 3 each
		long level = stock(701L, 5);

		// when: both at the same moment
		List<StockReserver.Result> results = together(2, i -> stockReserver.reserve(order(7100 + i, 701L, 3)));

		// then: one won, the other was told what is left; never 6 of 5
		assertThat(results).filteredOn(StockReserver.Reserved.class::isInstance).hasSize(1);
		assertThat(results).filteredOn(StockReserver.Failed.class::isInstance).singleElement()
				.extracting(result -> ((StockReserver.Failed) result).reason())
				.isEqualTo("Not enough stock of 'Concurrency 701' in " + warehouse.getCode() + ": only 2 available");
		assertThat(reserved(level)).isEqualTo(3);
	}

	@Test
	void tenOrdersForFiveItems_exactlyFiveAreReserved_andConflictsAreRetried() throws Exception {
		// given
		long level = stock(702L, 5);
		int retriesBefore = retryCounter.retries.get();

		// when: ten orders of 1, all at once
		List<StockReserver.Result> results = together(10, i -> stockReserver.reserve(order(7200 + i, 702L, 1)));

		// then: every order got an answer (no conflict reached the caller), and exactly the stock there was
		assertThat(results).filteredOn(StockReserver.Reserved.class::isInstance).hasSize(5);
		assertThat(results).filteredOn(StockReserver.Failed.class::isInstance).hasSize(5);
		assertThat(reserved(level)).isEqualTo(5);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM stock_reservations WHERE product_id = 702 AND warehouse_id = ?", Long.class,
				warehouse.getId())).isEqualTo(5);
		// and the collisions really happened: without them this test would prove nothing
		assertThat(retryCounter.retries.get()).isGreaterThan(retriesBefore);
	}

	@Test
	void tenAdjustmentsAtOnce_noneIsLost() throws Exception {
		// given
		long level = stock(703L, 0);
		ProductSnapshot product = new ProductSnapshot(703L, "SKU-703", "Concurrency 703", 0);

		// when: ten managers each add 1 at the same moment
		together(10, i -> stockAdjuster.apply(new AdjustStockRequest(703L, warehouse.getId(), 1, "count " + i), product));

		// then: 10, not 1 (a lost update) and no 409 for anyone
		assertThat(stockLevelRepository.findById(level).orElseThrow().getOnHand()).isEqualTo(10);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM stock_movements WHERE product_id = 703 AND warehouse_id = ? AND movement_type = 'ADJUST'",
				Long.class, warehouse.getId())).isEqualTo(10);
	}

	private long stock(long productId, int onHand) {
		StockLevel level = new StockLevel(warehouse, productId);
		level.copyProduct("SKU-" + productId, "Concurrency " + productId, 0);
		level.setOnHand(onHand);
		return stockLevelRepository.save(level).getId();
	}

	private int reserved(long levelId) {
		return stockLevelRepository.findById(levelId).orElseThrow().getReserved();
	}

	private EventEnvelope<OrderCreatedEvent> order(long orderId, long productId, int quantity) {
		return EventEnvelope.of(KafkaTopics.ORDER_CREATED, new OrderCreatedEvent(orderId, "SO-" + orderId, warehouse.getId(),
				List.of(new OrderCreatedEvent.Item(productId, "Concurrency " + productId, quantity))), Clock.systemUTC());
	}

	interface Task<T> {
		T run(int index) throws Exception;
	}

	/**
	 * Runs {@code count} tasks on their own threads, released together by a barrier, and returns their results. An
	 * exception in any task fails the test (Future.get rethrows it).
	 */
	private static <T> List<T> together(int count, Task<T> task) throws Exception {
		CyclicBarrier start = new CyclicBarrier(count);
		try (ExecutorService pool = Executors.newFixedThreadPool(count)) {
			List<Future<T>> futures = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				int index = i;
				Callable<T> call = () -> {
					start.await();
					return task.run(index);
				};
				futures.add(pool.submit(call));
			}
			List<T> results = new ArrayList<>();
			for (Future<T> future : futures) {
				results.add(future.get());
			}
			return results;
		}
	}
}
