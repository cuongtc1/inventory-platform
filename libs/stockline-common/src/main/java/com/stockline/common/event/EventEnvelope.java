package com.stockline.common.event;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * The JSON every Kafka message carries (docs/conventions.md section 6):
 *
 * <pre>
 * { "eventId": "6f1c2a1e-...", "eventType": "order.created", "occurredAt": "2026-10-22T08:15:30Z", "payload": { ... } }
 * </pre>
 *
 * @param eventId    unique per event; consumers remember it to skip a message Kafka delivers twice (T6.4)
 * @param eventType  what happened; the same as the topic name
 * @param occurredAt when it happened, in UTC
 * @param payload    the event's data; its record belongs to the service that publishes or reads it
 * @param <T>        the payload's type
 */
public record EventEnvelope<T>(
		UUID eventId,
		String eventType,
		Instant occurredAt,
		T payload
) {

	/** A new event: a random eventId, and "now" from the service's clock. */
	public static <T> EventEnvelope<T> of(String eventType, T payload, Clock clock) {
		return new EventEnvelope<>(UUID.randomUUID(), eventType, clock.instant(), payload);
	}
}
