package com.stockline.inventory;

import com.stockline.common.event.EventCodec;
import com.stockline.common.event.EventEnvelope;
import com.stockline.common.event.EventPublisher;
import com.stockline.common.event.KafkaTopics;
import com.stockline.inventory.entity.StockLevel;
import com.stockline.inventory.entity.Warehouse;
import com.stockline.inventory.event.OrderCreatedEvent;
import com.stockline.inventory.event.StockFailedEvent;
import com.stockline.inventory.event.StockReservedEvent;
import com.stockline.inventory.repository.StockLevelRepository;
import com.stockline.inventory.repository.WarehouseRepository;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * inventory-service's part of the saga, end to end on real MySQL and real Kafka: this test plays order-service. It
 * publishes {@code order.created} and reads the answer from {@code stock.reserved} or {@code stock.failed}.
 */
// The service's listeners on (off in tests by default, src/test/resources/config/application.yml)
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=true")
@Import(TestcontainersConfiguration.class)
class StockReservationSagaTest {

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private EventCodec eventCodec;

	@Autowired
	private ConsumerFactory<String, String> consumerFactory;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Autowired
	private StockLevelRepository stockLevelRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private KafkaAdmin kafkaAdmin;

	@Autowired
	private Clock clock;

	@Test
	void enoughStock_isReserved_andOrderServiceHearsIt() {
		// given: 10 mice and 3 cables in SG-01
		Warehouse warehouse = warehouseRepository.save(new Warehouse("SG-01", "Saga test", null));
		long mouse = stock(warehouse, 501L, "Saga Mouse", 10);
		long cable = stock(warehouse, 502L, "Saga Cable", 3);

		// when: order-service announces a new order
		eventPublisher.publish(KafkaTopics.ORDER_CREATED, "9001", new OrderCreatedEvent(9001, "SO-9001", warehouse.getId(),
				List.of(new OrderCreatedEvent.Item(501L, "Saga Mouse", 4), new OrderCreatedEvent.Item(502L, "Saga Cable", 3))));

		// then: the answer, keyed by the order id
		String answer = readAnswer(KafkaTopics.STOCK_RESERVED, "9001");
		assertThat(eventCodec.read(answer, StockReservedEvent.class).payload()).isEqualTo(new StockReservedEvent(9001));
		assertThat(stockLevelRepository.findById(mouse).orElseThrow().getReserved()).isEqualTo(4);
		assertThat(stockLevelRepository.findById(mouse).orElseThrow().getOnHand()).isEqualTo(10);
		assertThat(stockLevelRepository.findById(cable).orElseThrow().getAvailable()).isZero();
		assertThat(jdbcTemplate.queryForList(
				"SELECT CONCAT(product_id, ':', quantity, ':', status) FROM stock_reservations WHERE order_id = 9001 ORDER BY product_id",
				String.class)).containsExactly("501:4:RESERVED", "502:3:RESERVED");
		assertThat(jdbcTemplate.queryForList(
				"SELECT CONCAT(movement_type, ':', quantity, ':', reference) FROM stock_movements WHERE reference = 'SO-9001' ORDER BY product_id",
				String.class)).containsExactly("RESERVE:4:SO-9001", "RESERVE:3:SO-9001");
	}

	@Test
	void notEnoughStock_reservesNothing_andSaysWhy() {
		// given: 2 desks, and a lamp this warehouse never had
		Warehouse warehouse = warehouseRepository.save(new Warehouse("SG-02", "Saga test", null));
		long desk = stock(warehouse, 511L, "Saga Desk", 2);

		// when
		eventPublisher.publish(KafkaTopics.ORDER_CREATED, "9002", new OrderCreatedEvent(9002, "SO-9002", warehouse.getId(),
				List.of(new OrderCreatedEvent.Item(511L, "Saga Desk", 1), new OrderCreatedEvent.Item(512L, "Saga Lamp", 1))));

		// then: the reason names the line that was short; the desk that would fit isn't reserved either
		String answer = readAnswer(KafkaTopics.STOCK_FAILED, "9002");
		assertThat(eventCodec.read(answer, StockFailedEvent.class).payload())
				.isEqualTo(new StockFailedEvent(9002, "Not enough stock of 'Saga Lamp' in SG-02: only 0 available"));
		assertThat(stockLevelRepository.findById(desk).orElseThrow().getReserved()).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stock_reservations WHERE order_id = 9002", Long.class))
				.isZero();
	}

	@Test
	void replayOfAReservedOrder_changesNothing_andIsNotAnsweredAgain() throws Exception {
		// given: an order reserved once
		Warehouse warehouse = warehouseRepository.save(new Warehouse("SG-03", "Saga test", null));
		long mouse = stock(warehouse, 521L, "Saga Mouse", 10);
		EventEnvelope<OrderCreatedEvent> event = EventEnvelope.of(KafkaTopics.ORDER_CREATED, new OrderCreatedEvent(9003,
				"SO-9003", warehouse.getId(), List.of(new OrderCreatedEvent.Item(521L, "Saga Mouse", 4))), clock);
		eventPublisher.publish(KafkaTopics.ORDER_CREATED, "9003", event).get();
		readAnswer(KafkaTopics.STOCK_RESERVED, "9003");

		// when: Kafka delivers the same event again (the same eventId), and the service has handled it
		awaitConsumed(eventPublisher.publish(KafkaTopics.ORDER_CREATED, "9003", event).get());

		// then
		assertThat(stockLevelRepository.findById(mouse).orElseThrow().getReserved()).isEqualTo(4);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stock_movements WHERE reference = 'SO-9003'", Long.class))
				.isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM processed_events WHERE event_id = ?", Long.class,
				event.eventId().toString())).isEqualTo(1);
		assertThat(countMessages(KafkaTopics.STOCK_RESERVED, "9003")).isEqualTo(1);
	}

	@Test
	void replayOfARejectedOrder_afterNewStockArrived_stillReservesNothing() throws Exception {
		// given: rejected with 2 on hand, then 10 more arrive
		Warehouse warehouse = warehouseRepository.save(new Warehouse("SG-04", "Saga test", null));
		long desk = stock(warehouse, 531L, "Saga Desk", 2);
		EventEnvelope<OrderCreatedEvent> event = EventEnvelope.of(KafkaTopics.ORDER_CREATED, new OrderCreatedEvent(9004,
				"SO-9004", warehouse.getId(), List.of(new OrderCreatedEvent.Item(531L, "Saga Desk", 3))), clock);
		eventPublisher.publish(KafkaTopics.ORDER_CREATED, "9004", event).get();
		readAnswer(KafkaTopics.STOCK_FAILED, "9004");
		jdbcTemplate.update("UPDATE stock_levels SET on_hand = 12 WHERE id = ?", desk);

		// when: the old event comes again
		awaitConsumed(eventPublisher.publish(KafkaTopics.ORDER_CREATED, "9004", event).get());

		// then: order-service already rejected this order; reserving now would hold stock for an order nobody has
		assertThat(stockLevelRepository.findById(desk).orElseThrow().getReserved()).isZero();
		assertThat(countMessages(KafkaTopics.STOCK_RESERVED, "9004")).isZero();
	}

	/**
	 * Waits until the service's consumer group has committed past this message: then the listener has handled it.
	 * A redelivery leaves nothing else to wait for, since an idempotent consumer does nothing visible.
	 */
	private void awaitConsumed(SendResult<String, String> sent) throws Exception {
		TopicPartition partition = new TopicPartition(sent.getRecordMetadata().topic(), sent.getRecordMetadata().partition());
		long after = sent.getRecordMetadata().offset() + 1;
		try (AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
			long deadline = System.currentTimeMillis() + 30_000;
			while (System.currentTimeMillis() < deadline) {
				OffsetAndMetadata committed = admin.listConsumerGroupOffsets("inventory-service")
						.partitionsToOffsetAndMetadata().get().get(partition);
				if (committed != null && committed.offset() >= after) {
					return;
				}
				Thread.sleep(200);
			}
		}
		throw new AssertionError("inventory-service did not consume offset " + (after - 1) + " of " + partition);
	}

	/** How many messages with this key are on the topic, read from the start with a group of its own. */
	private int countMessages(String topic, String key) {
		try (Consumer<String, String> consumer = consumerFactory.createConsumer("test-" + UUID.randomUUID(), null)) {
			consumer.subscribe(List.of(topic));
			int count = 0;
			// Read until two polls in a row bring nothing new
			int emptyPolls = 0;
			while (emptyPolls < 2) {
				List<ConsumerRecord<String, String>> records = new ArrayList<>();
				KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(2)).records(topic).forEach(records::add);
				emptyPolls = records.isEmpty() ? emptyPolls + 1 : 0;
				count += (int) records.stream().filter(record -> key.equals(record.key())).count();
			}
			return count;
		}
	}

	private long stock(Warehouse warehouse, long productId, String name, int onHand) {
		StockLevel level = new StockLevel(warehouse, productId);
		level.copyProduct("SKU-" + productId, name, 0);
		level.setOnHand(onHand);
		return stockLevelRepository.save(level).getId();
	}

	/** The first message with this key on the topic, read with a consumer group of its own. */
	private String readAnswer(String topic, String key) {
		try (Consumer<String, String> consumer = consumerFactory.createConsumer("test-" + UUID.randomUUID(), null)) {
			consumer.subscribe(List.of(topic));
			long deadline = System.currentTimeMillis() + 30_000;
			while (System.currentTimeMillis() < deadline) {
				for (ConsumerRecord<String, String> record : KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(2)).records(topic)) {
					if (key.equals(record.key())) {
						return record.value();
					}
				}
			}
			throw new AssertionError("No message with key " + key + " on " + topic);
		}
	}
}
