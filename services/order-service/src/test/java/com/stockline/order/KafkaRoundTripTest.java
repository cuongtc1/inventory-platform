package com.stockline.order;

import com.stockline.common.event.EventCodec;
import com.stockline.common.event.EventEnvelope;
import com.stockline.common.event.EventPublisher;
import com.stockline.common.event.KafkaTopics;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * order-service against a real Kafka (Testcontainers): the topics exist after startup, and an event comes back as it
 * was sent. The order events themselves come with T6.2; the payload here is a stand-in.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class KafkaRoundTripTest {

	/** A stand-in payload; T6.2 declares the real order.created payload. */
	record Ping(String text) {
	}

	@Autowired
	private KafkaAdmin kafkaAdmin;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private EventCodec eventCodec;

	@Autowired
	private ConsumerFactory<String, String> consumerFactory;

	@Test
	void everyTopic_existsAfterStartup_withThreePartitions() {
		// when
		Map<String, TopicDescription> topics = kafkaAdmin.describeTopics(KafkaTopics.ALL.toArray(String[]::new));

		// then: created by the service itself (CommonKafkaAutoConfiguration), nobody ran kafka-topics.sh
		assertThat(topics).containsOnlyKeys(KafkaTopics.ALL);
		assertThat(topics.values()).allSatisfy(topic -> assertThat(topic.partitions()).hasSize(KafkaTopics.PARTITIONS));
	}

	@Test
	void publishedEvent_isReadBackWithKeyAndEnvelope() throws Exception {
		// given
		String key = "round-trip-" + UUID.randomUUID();

		// when: wait for the broker's acknowledgement
		eventPublisher.publish(KafkaTopics.ORDER_CREATED, key, new Ping("hello")).get();

		// then
		ConsumerRecord<String, String> record = readBack(KafkaTopics.ORDER_CREATED, key, 1).getFirst();
		EventEnvelope<Ping> envelope = eventCodec.read(record.value(), Ping.class);
		assertThat(envelope.eventType()).isEqualTo(KafkaTopics.ORDER_CREATED);
		assertThat(envelope.payload()).isEqualTo(new Ping("hello"));
		assertThat(envelope.eventId()).isNotNull();
	}

	@Test
	void sameKey_alwaysLandsInTheSamePartition() throws Exception {
		// given: one order's events, e.g. created and then cancelled
		String key = "same-key-" + UUID.randomUUID();
		List<Integer> partitions = new ArrayList<>();

		// when
		for (int i = 0; i < 5; i++) {
			SendResult<String, String> result = eventPublisher.publish(KafkaTopics.ORDER_CANCELLED, key, new Ping("event " + i)).get();
			partitions.add(result.getRecordMetadata().partition());
		}

		// then: one partition, so a consumer gets them in the order they were sent
		assertThat(partitions).containsOnly(partitions.getFirst());
		assertThat(readBack(KafkaTopics.ORDER_CANCELLED, key, 5))
				.extracting(record -> eventCodec.read(record.value(), Ping.class).payload().text())
				.containsExactly("event 0", "event 1", "event 2", "event 3", "event 4");
	}

	/**
	 * Reads a topic from its start with a consumer group of its own, and keeps the messages with this key: other tests
	 * share the broker. A new group with auto-offset-reset=earliest sees everything still stored in the topic.
	 */
	private List<ConsumerRecord<String, String>> readBack(String topic, String key, int expected) {
		try (Consumer<String, String> consumer = consumerFactory.createConsumer("test-" + UUID.randomUUID(), null)) {
			consumer.subscribe(List.of(topic));
			List<ConsumerRecord<String, String>> found = new ArrayList<>();
			long deadline = System.currentTimeMillis() + 15_000;
			while (found.size() < expected && System.currentTimeMillis() < deadline) {
				KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(2)).records(topic).forEach(record -> {
					if (key.equals(record.key())) {
						found.add(record);
					}
				});
			}
			assertThat(found).as("messages with key %s on %s", key, topic).hasSize(expected);
			return found;
		}
	}
}
