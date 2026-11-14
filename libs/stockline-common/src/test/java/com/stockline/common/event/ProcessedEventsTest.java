package com.stockline.common.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The claim logic without a database: a fake JdbcTemplate plays the unique key. The real MySQL statement is tested in
 * the services (ProcessedEventsMySqlTest).
 */
class ProcessedEventsTest {

	record Ping(String text) {
	}

	private static final Instant NOW = Instant.parse("2026-11-14T03:00:00Z");

	/** Remembers the event ids it was given and returns 0 rows for one it has seen, like INSERT IGNORE. */
	static class FakeJdbcTemplate extends JdbcTemplate {

		final Set<Object> eventIds = new HashSet<>();
		final List<Object[]> calls = new ArrayList<>();

		@Override
		public int update(String sql, Object... args) {
			calls.add(args);
			return eventIds.add(args[0]) ? 1 : 0;
		}
	}

	private final FakeJdbcTemplate jdbcTemplate = new FakeJdbcTemplate();
	private final ProcessedEvents processedEvents = new ProcessedEvents(jdbcTemplate,
			Clock.fixed(NOW, ZoneOffset.UTC), commonMessages());

	/** The real texts, so the log line of a duplicate is checked too. */
	private static ResourceBundleMessageSource commonMessages() {
		ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
		messages.setBasenames("common-messages");
		messages.setDefaultEncoding("UTF-8");
		messages.setFallbackToSystemLocale(false);
		return messages;
	}

	@AfterEach
	void endTransaction() {
		TransactionSynchronizationManager.setActualTransactionActive(false);
	}

	@Test
	void firstDelivery_isClaimed_aRedeliveryIsNot() {
		// given: what a transaction manager does when a transaction starts
		TransactionSynchronizationManager.setActualTransactionActive(true);
		EventEnvelope<Ping> event = new EventEnvelope<>(UUID.randomUUID(), "stock.low", NOW, new Ping("x"));

		// when / then
		assertThat(processedEvents.claim(event)).isTrue();
		assertThat(processedEvents.claim(event)).isFalse();
		// a different event, even with the same payload, is new
		assertThat(processedEvents.claim(EventEnvelope.of("stock.low", new Ping("x"), Clock.systemUTC()))).isTrue();
	}

	@Test
	void claim_storesIdTypeAndUtcTime_bySystem() {
		// given
		TransactionSynchronizationManager.setActualTransactionActive(true);
		UUID id = UUID.randomUUID();

		// when
		processedEvents.claim(new EventEnvelope<>(id, "order.created", NOW, new Ping("x")));

		// then: a LocalDateTime in UTC, not a Timestamp that the driver would shift to the JVM's zone
		LocalDateTime utc = LocalDateTime.of(2026, 11, 14, 3, 0);
		assertThat(jdbcTemplate.calls.getFirst()).containsExactly(id.toString(), "order.created", utc, utc, "system", "system");
	}

	@Test
	void outsideATransaction_itRefuses() {
		// The claim would commit on its own; if the work then failed, the event would be lost
		assertThatThrownBy(() -> processedEvents.claim(EventEnvelope.of("stock.low", new Ping("x"), Clock.systemUTC())))
				.isInstanceOf(IllegalStateException.class);
		assertThat(jdbcTemplate.calls).isEmpty();
	}
}
