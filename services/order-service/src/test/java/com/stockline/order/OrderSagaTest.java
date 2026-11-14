package com.stockline.order;

import com.jayway.jsonpath.JsonPath;
import com.stockline.common.event.EventCodec;
import com.stockline.common.event.EventEnvelope;
import com.stockline.common.event.EventPublisher;
import com.stockline.common.event.KafkaTopics;
import com.stockline.order.client.ProductClient;
import com.stockline.order.client.WarehouseClient;
import com.stockline.order.constant.OrderStatus;
import com.stockline.order.dto.OrderStatusHistoryResponse;
import com.stockline.order.dto.ProductSnapshot;
import com.stockline.order.dto.WarehouseSnapshot;
import com.stockline.order.entity.Customer;
import com.stockline.order.event.OrderCreatedEvent;
import com.stockline.order.event.StockFailedEvent;
import com.stockline.order.event.StockReservedEvent;
import com.stockline.order.repository.CustomerRepository;
import com.stockline.order.repository.SalesOrderRepository;
import com.stockline.order.service.OrderStatusService;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * order-service's part of the saga, end to end on real MySQL and real Kafka: this test plays inventory-service. Creating
 * an order publishes {@code order.created}; the answer on {@code stock.reserved} or {@code stock.failed} moves the order.
 */
// The service's listeners on (off in tests by default, src/test/resources/config/application.yml)
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=true")
// Security filters off: this class checks the saga; SecurityRulesTest checks the roles
@AutoConfigureMockMvc(addFilters = false)
@Import(TestcontainersConfiguration.class)
class OrderSagaTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private EventCodec eventCodec;

	@Autowired
	private ConsumerFactory<String, String> consumerFactory;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private SalesOrderRepository salesOrderRepository;

	@Autowired
	private OrderStatusService orderStatusService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private KafkaAdmin kafkaAdmin;

	@Autowired
	private Clock clock;

	@MockitoBean
	private ProductClient productClient;

	@MockitoBean
	private WarehouseClient warehouseClient;

	@BeforeEach
	void otherServices() {
		when(warehouseClient.getWarehouse(2L)).thenReturn(new WarehouseSnapshot(2L, "WH-01"));
		when(productClient.getProduct(6L)).thenReturn(new ProductSnapshot(6L, "A01", "Wireless Mouse", new BigDecimal("12.00"), "ACTIVE"));
	}

	@Test
	void createdOrder_isAnnounced_withItsLines() throws Exception {
		try (Consumer<String, String> consumer = listenFromNow(KafkaTopics.ORDER_CREATED)) {
			// when
			long id = createOrder("Saga Announce Ltd", 3);

			// then: inventory gets what it needs to reserve, keyed by the order id
			OrderCreatedEvent event = eventCodec.read(next(consumer, KafkaTopics.ORDER_CREATED, String.valueOf(id)),
					OrderCreatedEvent.class).payload();
			assertThat(event.orderId()).isEqualTo(id);
			assertThat(event.orderNumber()).startsWith("SO-");
			assertThat(event.warehouseId()).isEqualTo(2L);
			assertThat(event.items()).containsExactly(new OrderCreatedEvent.Item(6L, "A01", "Wireless Mouse", 3));
		}
	}

	@Test
	void stockReserved_movesTheOrderToReserved_bySystem() throws Exception {
		// given
		long id = createOrder("Saga Reserved Ltd", 1);

		// when: inventory's answer
		eventPublisher.publish(KafkaTopics.STOCK_RESERVED, String.valueOf(id), new StockReservedEvent(id)).get();

		// then
		awaitStatus(id, OrderStatus.RESERVED);
		OrderStatusHistoryResponse last = orderStatusService.history(id).getLast();
		assertThat(last.fromStatus()).isEqualTo(OrderStatus.PENDING);
		assertThat(last.createdBy()).isEqualTo("system");
	}

	@Test
	void stockFailed_rejectsTheOrder_withTheReason() throws Exception {
		// given
		long id = createOrder("Saga Rejected Ltd", 1);

		// when
		eventPublisher.publish(KafkaTopics.STOCK_FAILED, String.valueOf(id),
				new StockFailedEvent(id, "Not enough stock of 'Wireless Mouse' in WH-01: only 0 available")).get();

		// then
		awaitStatus(id, OrderStatus.REJECTED);
		assertThat(orderStatusService.history(id).getLast().note())
				.isEqualTo("Not enough stock of 'Wireless Mouse' in WH-01: only 0 available");
	}

	@Test
	void replayOfStockFailed_isAppliedOnce() throws Exception {
		// given: an order rejected once
		long id = createOrder("Saga Replay Ltd", 1);
		EventEnvelope<StockFailedEvent> event = EventEnvelope.of(KafkaTopics.STOCK_FAILED,
				new StockFailedEvent(id, "Not enough stock of 'Wireless Mouse' in WH-01: only 0 available"), clock);
		eventPublisher.publish(KafkaTopics.STOCK_FAILED, String.valueOf(id), event).get();
		awaitStatus(id, OrderStatus.REJECTED);

		// when: Kafka delivers the same event again, and the service has handled it
		awaitConsumed(eventPublisher.publish(KafkaTopics.STOCK_FAILED, String.valueOf(id), event).get());

		// then: one REJECTED row, and the event is remembered once
		assertThat(orderStatusService.history(id)).extracting(OrderStatusHistoryResponse::toStatus)
				.containsExactly(OrderStatus.PENDING, OrderStatus.REJECTED);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM processed_events WHERE event_id = ?", Long.class,
				event.eventId().toString())).isEqualTo(1);
	}

	/** Waits until the service's consumer group has committed past this message: then the listener has handled it. */
	private void awaitConsumed(SendResult<String, String> sent) throws Exception {
		TopicPartition partition = new TopicPartition(sent.getRecordMetadata().topic(), sent.getRecordMetadata().partition());
		long after = sent.getRecordMetadata().offset() + 1;
		try (AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
			long deadline = System.currentTimeMillis() + 30_000;
			while (System.currentTimeMillis() < deadline) {
				OffsetAndMetadata committed = admin.listConsumerGroupOffsets("order-service")
						.partitionsToOffsetAndMetadata().get().get(partition);
				if (committed != null && committed.offset() >= after) {
					return;
				}
				Thread.sleep(200);
			}
		}
		throw new AssertionError("order-service did not consume offset " + (after - 1) + " of " + partition);
	}

	private long createOrder(String customerName, int quantity) throws Exception {
		long customerId = customerRepository.save(new Customer(customerName, null, null)).getId();
		String body = mockMvc.perform(post("/api/sales-orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "customerId": %d, "warehouseId": 2, "items": [ { "productId": 6, "quantity": %d } ] }
								""".formatted(customerId, quantity)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	/** The listener runs on its own thread, so the test waits for the result instead of expecting it at once. */
	private void awaitStatus(long id, OrderStatus expected) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 30_000;
		while (System.currentTimeMillis() < deadline) {
			if (salesOrderRepository.findById(id).orElseThrow().getStatus() == expected) {
				return;
			}
			Thread.sleep(200);
		}
		assertThat(salesOrderRepository.findById(id).orElseThrow().getStatus()).isEqualTo(expected);
	}

	/**
	 * A consumer that sees only messages sent from now on. Needed because every test context has its own MySQL, so
	 * order ids (the keys) repeat across contexts, while all of them share one Kafka: key "3" on order.created may be
	 * another test's order 3. Positioning at the end first means the next message with our key is ours.
	 */
	private Consumer<String, String> listenFromNow(String topic) {
		Consumer<String, String> consumer = consumerFactory.createConsumer("test-" + UUID.randomUUID(), null);
		consumer.subscribe(List.of(topic));
		// Partitions are assigned during poll(); wait for that, then jump to the end of each one
		long deadline = System.currentTimeMillis() + 30_000;
		while (consumer.assignment().isEmpty() && System.currentTimeMillis() < deadline) {
			consumer.poll(Duration.ofMillis(200));
		}
		consumer.seekToEnd(consumer.assignment());
		// position() makes the seek happen now, before the test sends anything
		consumer.assignment().forEach(consumer::position);
		return consumer;
	}

	/** The next message with this key. */
	private static String next(Consumer<String, String> consumer, String topic, String key) {
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
