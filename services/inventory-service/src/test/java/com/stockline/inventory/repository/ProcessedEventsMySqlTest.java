package com.stockline.inventory.repository;

import com.stockline.common.event.EventEnvelope;
import com.stockline.common.event.ProcessedEvents;
import com.stockline.inventory.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * stockline-common's ProcessedEvents against this service's real {@code processed_events} table: the INSERT IGNORE
 * statement only exists in MySQL. A @DataJpaTest runs each test in a transaction, as a consumer does.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ProcessedEventsMySqlTest {

	record Ping(String text) {
	}

	private static final Instant NOW = Instant.parse("2026-11-14T03:00:00.123456Z");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void sameEventTwice_onlyTheFirstIsClaimed_andStoredOnce() {
		// given
		ProcessedEvents processedEvents = new ProcessedEvents(jdbcTemplate, Clock.fixed(NOW, ZoneOffset.UTC),
				new StaticMessageSource() {
					{
						setUseCodeAsDefaultMessage(true);
					}
				});
		EventEnvelope<Ping> event = new EventEnvelope<>(UUID.randomUUID(), "order.created", NOW, new Ping("x"));

		// when / then
		assertThat(processedEvents.claim(event)).isTrue();
		assertThat(processedEvents.claim(event)).isFalse();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM processed_events WHERE event_id = ?", Long.class,
				event.eventId().toString())).isEqualTo(1);
		// stored as UTC, to the microsecond
		assertThat(jdbcTemplate.queryForObject("SELECT created_at FROM processed_events WHERE event_id = ?",
				LocalDateTime.class, event.eventId().toString()))
				.isEqualTo(LocalDateTime.of(2026, 11, 14, 3, 0, 0, 123_456_000));
	}
}
