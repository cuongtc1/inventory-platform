package com.stockline.common.event;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.mock.MockProducerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No broker: Kafka's own {@link MockProducer} records what would have been sent, behind a real KafkaTemplate.
 */
class EventPublisherTest {

	record OrderIdOnly(long orderId) {
	}

	private static final Instant NOW = Instant.parse("2026-11-12T03:00:00Z");

	// KafkaTemplate closes its producer after each send, so each send gets a fresh MockProducer
	private final List<MockProducer<String, String>> producers = new ArrayList<>();
	private final EventCodec codec = new EventCodec(JsonMapper.builder().build());
	private final EventPublisher publisher = new EventPublisher(
			new KafkaTemplate<>(new MockProducerFactory<>(() -> {
				MockProducer<String, String> producer =
						new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
				producers.add(producer);
				return producer;
			})),
			codec, Clock.fixed(NOW, ZoneOffset.UTC));

	/** Everything sent so far, oldest first. */
	private List<ProducerRecord<String, String>> sent() {
		return producers.stream().flatMap(producer -> producer.history().stream()).toList();
	}

	@Test
	void publish_sendsTheEnvelopeAsJsonWithTheKey() {
		// when
		publisher.publish(KafkaTopics.ORDER_CREATED, "42", new OrderIdOnly(42)).join();

		// then
		assertThat(sent()).singleElement().satisfies(record -> {
			assertThat(record.topic()).isEqualTo("order.created");
			assertThat(record.key()).isEqualTo("42");
			EventEnvelope<OrderIdOnly> envelope = codec.read(record.value(), OrderIdOnly.class);
			assertThat(envelope.eventType()).isEqualTo("order.created");
			assertThat(envelope.occurredAt()).isEqualTo(NOW);
			assertThat(envelope.eventId()).isNotNull();
			assertThat(envelope.payload()).isEqualTo(new OrderIdOnly(42));
		});
	}

	@Test
	void publish_twoEvents_haveDifferentIds() {
		// when
		publisher.publish(KafkaTopics.ORDER_CREATED, "42", new OrderIdOnly(42)).join();
		publisher.publish(KafkaTopics.ORDER_CREATED, "42", new OrderIdOnly(42)).join();

		// then: the same fact sent twice is two events; a consumer tells a redelivery apart by its id (T6.4)
		assertThat(sent()).extracting(ProducerRecord::value)
				.map(json -> codec.read(json, OrderIdOnly.class).eventId())
				.doesNotHaveDuplicates();
	}

	@Test
	void publish_existingEnvelope_keepsItsId() {
		// given: e.g. one read back from the outbox (T6.5)
		EventEnvelope<OrderIdOnly> stored = EventEnvelope.of(KafkaTopics.ORDER_CANCELLED, new OrderIdOnly(7), Clock.systemUTC());

		// when
		publisher.publish(KafkaTopics.ORDER_CANCELLED, "7", stored).join();

		// then
		assertThat(codec.read(sent().getFirst().value(), OrderIdOnly.class).eventId())
				.isEqualTo(stored.eventId());
	}
}
