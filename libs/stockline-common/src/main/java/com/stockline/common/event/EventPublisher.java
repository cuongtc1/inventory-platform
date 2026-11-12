package com.stockline.common.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Clock;
import java.util.concurrent.CompletableFuture;

/**
 * Sends events to Kafka in the shared envelope. The key is the id of the main entity (for order events, the order
 * id): Kafka sends messages with the same key to the same partition, so one order's events stay in order.
 *
 * <p>Sending is asynchronous: the returned future completes when the broker has stored the message (acks=all), or
 * fails. From T6.5 on, events go through an outbox table first, and the outbox publisher calls this class.
 */
public class EventPublisher {

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final EventCodec eventCodec;
	private final Clock clock;

	public EventPublisher(KafkaTemplate<String, String> kafkaTemplate, EventCodec eventCodec, Clock clock) {
		this.kafkaTemplate = kafkaTemplate;
		this.eventCodec = eventCodec;
		this.clock = clock;
	}

	/** Wraps the payload in a new envelope (eventType = the topic) and sends it. */
	public CompletableFuture<SendResult<String, String>> publish(String topic, String key, Object payload) {
		return publish(topic, key, EventEnvelope.of(topic, payload, clock));
	}

	/** Sends an envelope that already exists, e.g. one stored in the outbox. */
	public CompletableFuture<SendResult<String, String>> publish(String topic, String key, EventEnvelope<?> envelope) {
		return kafkaTemplate.send(topic, key, eventCodec.write(envelope));
	}
}
