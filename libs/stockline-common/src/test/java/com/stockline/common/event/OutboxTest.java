package com.stockline.common.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Writing to the outbox, without a database: a fake JdbcTemplate records the INSERT. Sending is tested on real MySQL in
 * inventory-service (OutboxMySqlTest).
 */
class OutboxTest {

	record OrderIdOnly(long orderId) {
	}

	private static final Instant NOW = Instant.parse("2026-11-14T03:00:00Z");

	/** Records the arguments of each update. */
	static class FakeJdbcTemplate extends JdbcTemplate {

		final List<Object[]> inserts = new ArrayList<>();

		@Override
		public int update(String sql, Object... args) {
			inserts.add(args);
			return 1;
		}
	}

	private final FakeJdbcTemplate jdbcTemplate = new FakeJdbcTemplate();
	private final EventCodec codec = new EventCodec(JsonMapper.builder().build());
	private final Outbox outbox = new Outbox(jdbcTemplate, codec, Clock.fixed(NOW, ZoneOffset.UTC));

	@AfterEach
	void endTransaction() {
		TransactionSynchronizationManager.setActualTransactionActive(false);
	}

	@Test
	void add_storesTheWholeEnvelope_withItsKey() {
		// given: what a transaction manager does when a transaction starts
		TransactionSynchronizationManager.setActualTransactionActive(true);

		// when
		EventEnvelope<Object> stored = outbox.add(KafkaTopics.ORDER_CREATED, "42", new OrderIdOnly(42));

		// then: the message is the envelope as it will be sent, with the id every resend keeps
		Object[] row = jdbcTemplate.inserts.getFirst();
		assertThat(row[0]).isEqualTo(stored.eventId().toString());
		assertThat(row[1]).isEqualTo("order.created");
		assertThat(row[2]).isEqualTo("42");
		EventEnvelope<OrderIdOnly> message = codec.read((String) row[3], OrderIdOnly.class);
		assertThat(message.eventId()).isEqualTo(stored.eventId());
		assertThat(message.eventType()).isEqualTo("order.created");
		assertThat(message.occurredAt()).isEqualTo(NOW);
		assertThat(message.payload()).isEqualTo(new OrderIdOnly(42));
		// created_at in UTC, by system
		assertThat(row).endsWith(LocalDateTime.of(2026, 11, 14, 3, 0), LocalDateTime.of(2026, 11, 14, 3, 0), "system", "system");
	}

	@Test
	void add_outsideATransaction_refuses() {
		// The event would be stored even if the change it announces then failed
		assertThatThrownBy(() -> outbox.add(KafkaTopics.ORDER_CREATED, "42", new OrderIdOnly(42)))
				.isInstanceOf(IllegalStateException.class);
		assertThat(jdbcTemplate.inserts).isEmpty();
	}
}
