package com.stockline.inventory;

import com.stockline.common.event.EventCodec;
import com.stockline.common.event.EventEnvelope;
import com.stockline.common.event.EventPublisher;
import com.stockline.common.event.KafkaTopics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * inventory-service consuming with {@code @KafkaListener} against a real Kafka (Testcontainers). Two listeners in two
 * consumer groups show what a group is: each group gets every message once, so two services can both react to the
 * same event. The real consumers come with T6.2; these listeners exist only in this test.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class KafkaListenerTest {

	/** A stand-in payload; the real stock.low payload comes with its task. */
	record Ping(String text) {
	}

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private Listeners listeners;

	@Value("${spring.kafka.consumer.group-id}")
	private String serviceGroup;

	@Test
	void theServicesConsumerGroup_isItsName() {
		// docs/conventions.md section 6: one group per service
		assertThat(serviceGroup).isEqualTo("inventory-service");
	}

	@Test
	void everyGroup_getsTheEventOnce_asATypedPayload() throws Exception {
		// given: a text no other test sends
		String text = "low-" + UUID.randomUUID();

		// when
		eventPublisher.publish(KafkaTopics.STOCK_LOW, "12", new Ping(text)).get();

		// then: both groups received it; the listeners may also see other tests' messages, so look for this one
		assertThat(nextWith(listeners.groupA, text)).isNotNull();
		assertThat(nextWith(listeners.groupB, text)).isNotNull();
	}

	private static EventEnvelope<Ping> nextWith(BlockingQueue<EventEnvelope<Ping>> queue, String text)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + 20_000;
		while (System.currentTimeMillis() < deadline) {
			EventEnvelope<Ping> envelope = queue.poll(1, TimeUnit.SECONDS);
			if (envelope != null && envelope.payload().text().equals(text)) {
				return envelope;
			}
		}
		return null;
	}

	/**
	 * Test-only listeners. {@code groupId} overrides the service's group, so each listener is a consumer group of its
	 * own. Spring calls the method for every message, on a listener thread; the queues hand them to the test.
	 */
	@TestConfiguration
	static class Listeners {

		final BlockingQueue<EventEnvelope<Ping>> groupA = new LinkedBlockingQueue<>();
		final BlockingQueue<EventEnvelope<Ping>> groupB = new LinkedBlockingQueue<>();

		private final EventCodec eventCodec;

		Listeners(EventCodec eventCodec) {
			this.eventCodec = eventCodec;
		}

		@KafkaListener(topics = KafkaTopics.STOCK_LOW, groupId = "kafka-listener-test-a")
		void onStockLowA(String json) {
			groupA.add(eventCodec.read(json, Ping.class));
		}

		@KafkaListener(topics = KafkaTopics.STOCK_LOW, groupId = "kafka-listener-test-b")
		void onStockLowB(String json) {
			groupB.add(eventCodec.read(json, Ping.class));
		}
	}
}
