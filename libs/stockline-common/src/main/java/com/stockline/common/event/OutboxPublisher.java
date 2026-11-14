package com.stockline.common.event;

import com.stockline.common.constant.CommonMessageKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Sends what {@link Outbox} stored: every {@code stockline.outbox.publish-interval} milliseconds (default 1000), the
 * oldest unsent rows, one after the other, each marked as published once Kafka has acknowledged it.
 *
 * <ul>
 *     <li><b>At least once.</b> If the service stops after Kafka acknowledged a message but before the row was marked,
 *     the message is sent again on the next run. Consumers are idempotent (ProcessedEvents), and the message is the
 *     same text with the same eventId, so the second copy is skipped.</li>
 *     <li><b>In order.</b> Rows go out by id; the first failure ends the run, and the next run starts at that row
 *     again. So a later event of an order never overtakes an earlier one.</li>
 *     <li><b>No transaction</b> around the sending: a slow broker mustn't hold a database connection and locks.</li>
 *     <li><b>One instance assumed.</b> Two instances of a service would both send the same rows; harmless for
 *     idempotent consumers, but double the traffic. A larger system locks rows (SELECT ... FOR UPDATE SKIP LOCKED) or
 *     elects one publisher.</li>
 * </ul>
 *
 * Published rows stay in the table as a record of what was sent; cleaning old ones up is not part of this project yet.
 */
public class OutboxPublisher {

	private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

	/** Rows per run; the rest wait for the next run. */
	static final int BATCH_SIZE = 100;

	/** How long to wait for Kafka's acknowledgement of one message. */
	static final long SEND_TIMEOUT_SECONDS = 10;

	private static final String SELECT_PENDING = """
			SELECT id, event_id, topic, message_key, message FROM outbox_events
			WHERE published_at IS NULL ORDER BY id LIMIT ?""";

	private static final String MARK_PUBLISHED = "UPDATE outbox_events SET published_at = ?, updated_at = ? WHERE id = ?";

	/** One row of outbox_events that hasn't been sent yet. */
	private record Pending(long id, String eventId, String topic, String key, String message) {
	}

	private final JdbcTemplate jdbcTemplate;
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final Clock clock;
	private final MessageSource messageSource;

	public OutboxPublisher(JdbcTemplate jdbcTemplate, KafkaTemplate<String, String> kafkaTemplate, Clock clock,
						   MessageSource messageSource) {
		this.jdbcTemplate = jdbcTemplate;
		this.kafkaTemplate = kafkaTemplate;
		this.clock = clock;
		this.messageSource = messageSource;
	}

	@Scheduled(fixedDelayString = "${stockline.outbox.publish-interval:1000}")
	void scheduledRun() {
		publishPending();
	}

	/**
	 * Sends the oldest unsent rows, in order, until one fails or the batch is done.
	 *
	 * @return how many were sent and marked
	 */
	public int publishPending() {
		List<Pending> pending = jdbcTemplate.query(SELECT_PENDING, (row, number) -> new Pending(row.getLong("id"),
				row.getString("event_id"), row.getString("topic"), row.getString("message_key"), row.getString("message")),
				BATCH_SIZE);
		int published = 0;
		for (Pending event : pending) {
			try {
				kafkaTemplate.send(event.topic(), event.key(), event.message()).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return published;
			} catch (Exception e) {
				// Kafka down or too slow: stop here, so nothing later overtakes this event; the next run tries again
				log.warn(messageSource.getMessage(CommonMessageKeys.SYSTEM_OUTBOX_PUBLISH_FAILED,
						new Object[] {event.topic(), event.eventId(), e.getMessage()}, Locale.ROOT));
				return published;
			}
			LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
			jdbcTemplate.update(MARK_PUBLISHED, now, now, event.id());
			published++;
		}
		return published;
	}
}
