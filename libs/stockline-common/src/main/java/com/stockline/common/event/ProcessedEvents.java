package com.stockline.common.event;

import com.stockline.common.constant.AuditConstants;
import com.stockline.common.constant.CommonMessageKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

/**
 * Makes a Kafka consumer idempotent: an event is applied at most once, even when Kafka delivers it again (after a
 * crash, a rebalance, or a retry). Every consuming service has the table {@code processed_events} with a unique
 * {@code event_id} (docs/conventions.md section 6).
 *
 * <p>Usage, inside the transaction that applies the event:
 *
 * <pre>
 * if (!processedEvents.claim(envelope)) {
 *     return; // seen before: its effects are already stored
 * }
 * ... apply the event ...
 * </pre>
 *
 * The claim and the work commit together or not at all: if the work fails, the claim is rolled back too, and the
 * next delivery tries again.
 */
public class ProcessedEvents {

	private static final Logger log = LoggerFactory.getLogger(ProcessedEvents.class);

	/**
	 * One statement to check and claim. {@code INSERT IGNORE} (MySQL) adds the row, or adds nothing when the unique
	 * key {@code uk_processed_events_event_id} already has this id, and says which by the row count. A second
	 * delivery running at the same moment waits for the first one's lock, then finds the row.
	 */
	private static final String CLAIM = """
			INSERT IGNORE INTO processed_events (event_id, event_type, created_at, updated_at, created_by, updated_by)
			VALUES (?, ?, ?, ?, ?, ?)""";

	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;
	private final MessageSource messageSource;

	public ProcessedEvents(JdbcTemplate jdbcTemplate, Clock clock, MessageSource messageSource) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
		this.messageSource = messageSource;
	}

	/**
	 * Records the event as processed.
	 *
	 * @return true the first time; false when it was processed before, so the caller skips it
	 * @throws IllegalStateException outside a transaction: the claim would be stored even if the work then failed,
	 *                               and the event would be lost
	 */
	public boolean claim(EventEnvelope<?> envelope) {
		if (!TransactionSynchronizationManager.isActualTransactionActive()) {
			throw new IllegalStateException("ProcessedEvents.claim must run inside the transaction that applies the event");
		}
		// LocalDateTime in UTC: written as it is; a java.sql.Timestamp would be shifted to the JVM's time zone
		LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
		int inserted = jdbcTemplate.update(CLAIM, envelope.eventId().toString(), envelope.eventType(), now, now,
				AuditConstants.SYSTEM_USER, AuditConstants.SYSTEM_USER);
		if (inserted == 0) {
			log.info(messageSource.getMessage(CommonMessageKeys.SYSTEM_EVENT_DUPLICATE,
					new Object[] {envelope.eventType(), envelope.eventId()}, Locale.ROOT));
		}
		return inserted == 1;
	}
}
