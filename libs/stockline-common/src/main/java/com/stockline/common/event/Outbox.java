package com.stockline.common.event;

import com.stockline.common.constant.AuditConstants;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * The transactional outbox: instead of sending an event to Kafka, a service stores it in its own table
 * {@code outbox_events}, in the same transaction as the change the event announces. {@link OutboxPublisher} sends it
 * afterwards. So an order is never saved without its event, and no event announces an order that was rolled back,
 * even when Kafka is down (docs/conventions.md section 6).
 *
 * <p>The stored message is the whole envelope as JSON, with its eventId. Every resend sends exactly that text, so a
 * consumer that already applied it recognizes the id (ProcessedEvents) and skips it.
 */
public class Outbox {

	private static final String INSERT = """
			INSERT INTO outbox_events (event_id, topic, message_key, message, created_at, updated_at, created_by, updated_by)
			VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";

	private final JdbcTemplate jdbcTemplate;
	private final EventCodec eventCodec;
	private final Clock clock;

	public Outbox(JdbcTemplate jdbcTemplate, EventCodec eventCodec, Clock clock) {
		this.jdbcTemplate = jdbcTemplate;
		this.eventCodec = eventCodec;
		this.clock = clock;
	}

	/**
	 * Stores a new event for {@code topic}: a new envelope around the payload (eventType = the topic).
	 *
	 * @param key the message key, the id of the main entity (for order events, the order id)
	 * @return the stored envelope
	 * @throws IllegalStateException outside a transaction: the event would be stored even if the change it announces
	 *                               then failed
	 */
	public EventEnvelope<Object> add(String topic, String key, Object payload) {
		if (!TransactionSynchronizationManager.isActualTransactionActive()) {
			throw new IllegalStateException("Outbox.add must run inside the transaction that makes the change");
		}
		EventEnvelope<Object> envelope = EventEnvelope.of(topic, payload, clock);
		// LocalDateTime in UTC: written as it is; a java.sql.Timestamp would be shifted to the JVM's time zone
		LocalDateTime now = LocalDateTime.ofInstant(envelope.occurredAt(), ZoneOffset.UTC);
		jdbcTemplate.update(INSERT, envelope.eventId().toString(), topic, key, eventCodec.write(envelope), now, now,
				AuditConstants.SYSTEM_USER, AuditConstants.SYSTEM_USER);
		return envelope;
	}
}
