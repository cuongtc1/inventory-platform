package com.stockline.order.service;

import com.stockline.order.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The counter on real MySQL, with real transactions from several threads at once. NOT_SUPPORTED on the class: this
 * test must not wrap everything in the one rolled-back transaction a {@code @DataJpaTest} normally uses, because the
 * threads need transactions of their own that really commit.
 */
@DataJpaTest
@Import({TestcontainersConfiguration.class, OrderNumberGenerator.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OrderNumberGeneratorTest {

	private static final int THREADS = 8;
	private static final int NUMBERS_PER_THREAD = 5;

	@Autowired
	private OrderNumberGenerator generator;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	void numbers_followTheFormat() {
		String number = new TransactionTemplate(transactionManager).execute(status -> generator.nextSalesOrderNumber());

		assertThat(number).matches("SO-\\d{4,}");
	}

	@Test
	void manyThreadsAtOnce_neverGetTheSameNumber_andNoneIsSkipped() throws Exception {
		// given: each call takes a number in its own transaction, like each order does
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);
		Callable<List<Long>> worker = () -> {
			List<Long> taken = new ArrayList<>();
			for (int i = 0; i < NUMBERS_PER_THREAD; i++) {
				taken.add(Long.parseLong(transaction.execute(status -> generator.nextSalesOrderNumber()).substring(3)));
			}
			return taken;
		};

		// when
		List<Long> all = Collections.synchronizedList(new ArrayList<>());
		try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
			List<Future<List<Long>>> results = new ArrayList<>();
			for (int i = 0; i < THREADS; i++) {
				results.add(pool.submit(worker));
			}
			for (Future<List<Long>> result : results) {
				all.addAll(result.get());
			}
		}

		// then: 40 different numbers in one unbroken run
		assertThat(all).doesNotHaveDuplicates().hasSize(THREADS * NUMBERS_PER_THREAD);
		long first = Collections.min(all);
		assertThat(Collections.max(all)).isEqualTo(first + THREADS * NUMBERS_PER_THREAD - 1);
	}

	@Test
	void withoutATransaction_itRefuses() {
		// A number taken in its own short transaction would lose the lock before the order is saved
		assertThatThrownBy(() -> generator.nextSalesOrderNumber())
				.isInstanceOf(IllegalTransactionStateException.class);
	}
}
