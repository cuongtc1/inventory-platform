package com.stockline.inventory.repository;

import com.stockline.common.event.EventCodec;
import com.stockline.common.event.KafkaTopics;
import com.stockline.common.event.Outbox;
import com.stockline.common.event.OutboxPublisher;
import com.stockline.inventory.TestcontainersConfiguration;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.mock.MockProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * stockline-common's Outbox and OutboxPublisher on this service's real {@code outbox_events} table, with real
 * transactions that commit (NOT_SUPPORTED: no rolled-back test transaction around the test). Kafka is replaced by
 * MockProducer, or by a template whose sends fail, to play "Kafka is down".
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxMySqlTest {

	record Ping(int number) {
	}

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private final EventCodec codec = new EventCodec(JsonMapper.builder().build());
	private final StaticMessageSource messages = new StaticMessageSource() {
		{
			setUseCodeAsDefaultMessage(true);
		}
	};

	/** Every MockProducer the working template used; KafkaTemplate closes its producer after each send. */
	private final List<MockProducer<String, String>> producers = new ArrayList<>();

	private Outbox outbox;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM outbox_events");
		outbox = new Outbox(jdbcTemplate, codec, Clock.systemUTC());
	}

	@Test
	void eventOfARolledBackChange_isNeverStored() {
		// when: the change fails after its event was added
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			outbox.add(KafkaTopics.STOCK_RESERVED, "1", new Ping(1));
			status.setRollbackOnly();
		});

		// then: nothing to send; an event never announces something that didn't happen
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events", Long.class)).isZero();
	}

	@Test
	void publisher_sendsOldestFirst_andMarksEachOnce() {
		// given
		List<String> ids = addEvents(3);
		OutboxPublisher publisher = new OutboxPublisher(jdbcTemplate, workingKafka(), Clock.systemUTC(), messages);

		// when
		int first = publisher.publishPending();
		int second = publisher.publishPending();

		// then: in order, the stored text as it is, and nothing twice
		assertThat(first).isEqualTo(3);
		assertThat(second).isZero();
		assertThat(sent()).extracting(record -> codec.read(record.value(), Ping.class).eventId().toString())
				.containsExactlyElementsOf(ids);
		assertThat(sent()).extracting(ProducerRecord::key).containsExactly("1", "2", "3");
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events WHERE published_at IS NULL", Long.class))
				.isZero();
	}

	@Test
	void kafkaDown_nothingIsLost_andItGoesOutInOrderLater() {
		// given
		List<String> ids = addEvents(3);

		// when: Kafka refuses everything
		int whileDown = new OutboxPublisher(jdbcTemplate, failingKafka(), Clock.systemUTC(), messages).publishPending();

		// then: still waiting, all three
		assertThat(whileDown).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events WHERE published_at IS NULL", Long.class))
				.isEqualTo(3);

		// when: Kafka is back
		int afterwards = new OutboxPublisher(jdbcTemplate, workingKafka(), Clock.systemUTC(), messages).publishPending();

		// then
		assertThat(afterwards).isEqualTo(3);
		assertThat(sent()).extracting(record -> codec.read(record.value(), Ping.class).eventId().toString())
				.containsExactlyElementsOf(ids);
	}

	/** Adds events in committed transactions, like real changes; returns their ids in order. */
	private List<String> addEvents(int count) {
		List<String> ids = new ArrayList<>();
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);
		for (int i = 1; i <= count; i++) {
			int number = i;
			ids.add(transaction.execute(status ->
					outbox.add(KafkaTopics.STOCK_RESERVED, String.valueOf(number), new Ping(number)).eventId().toString()));
		}
		return ids;
	}

	private KafkaTemplate<String, String> workingKafka() {
		return new KafkaTemplate<>(new MockProducerFactory<>(() -> {
			MockProducer<String, String> producer = new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
			producers.add(producer);
			return producer;
		}));
	}

	/** A broker that is down: every send fails. */
	private KafkaTemplate<String, String> failingKafka() {
		return new KafkaTemplate<>(new MockProducerFactory<>(() -> new MockProducer<>(true, null, new StringSerializer(),
				new StringSerializer()))) {
			@Override
			public CompletableFuture<SendResult<String, String>> send(String topic, String key, String data) {
				return CompletableFuture.failedFuture(new KafkaException("broker not available"));
			}
		};
	}

	private List<ProducerRecord<String, String>> sent() {
		return producers.stream().flatMap(producer -> producer.history().stream()).toList();
	}
}
