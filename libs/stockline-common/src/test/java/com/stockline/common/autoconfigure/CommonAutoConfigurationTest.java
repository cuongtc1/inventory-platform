package com.stockline.common.autoconfigure;

import com.stockline.common.event.EventCodec;
import com.stockline.common.event.EventPublisher;
import com.stockline.common.event.KafkaTopics;
import com.stockline.common.event.Outbox;
import com.stockline.common.event.OutboxPublisher;
import com.stockline.common.event.ProcessedEvents;
import com.stockline.common.exception.GlobalExceptionHandler;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks which beans the auto-configurations create, without starting a whole application.
 * {@code ApplicationContextRunner} builds a small Spring context per test with exactly the given configurations.
 */
class CommonAutoConfigurationTest {

	private final ApplicationContextRunner plainContext = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					CommonTimeAutoConfiguration.class, CommonWebAutoConfiguration.class));

	private final WebApplicationContextRunner webContext = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					CommonTimeAutoConfiguration.class, CommonWebAutoConfiguration.class));

	@Test
	void clock_isUtcSystemClock() {
		plainContext.run(context -> assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneOffset.UTC));
	}

	@Test
	void clock_definedByTheService_replacesTheDefault() {
		Clock fixed = Clock.fixed(Instant.parse("2026-10-23T08:00:00Z"), ZoneOffset.UTC);

		plainContext.withBean(Clock.class, () -> fixed)
				.run(context -> assertThat(context.getBean(Clock.class)).isSameAs(fixed));
	}

	@Test
	void exceptionHandler_onlyInAServletWebApplication() {
		webContext.run(context -> assertThat(context).hasSingleBean(GlobalExceptionHandler.class));
		plainContext.run(context -> assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class));
	}

	private final ApplicationContextRunner kafkaContext = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CommonTimeAutoConfiguration.class, JacksonAutoConfiguration.class,
					KafkaAutoConfiguration.class, CommonKafkaAutoConfiguration.class));

	@Test
	void kafka_publisherCodecAndEveryTopic() {
		kafkaContext.run(context -> assertThat(context)
				.hasSingleBean(EventPublisher.class)
				.hasSingleBean(EventCodec.class)
				.hasSingleBean(KafkaAdmin.NewTopics.class));
		assertThat(CommonKafkaAutoConfiguration.topics())
				.extracting(NewTopic::name)
				.containsExactlyInAnyOrderElementsOf(KafkaTopics.ALL);
		assertThat(CommonKafkaAutoConfiguration.topics())
				.allSatisfy(topic -> assertThat(topic.numPartitions()).isEqualTo(KafkaTopics.PARTITIONS));
	}

	@Test
	void kafka_processedEventsAndOutbox_onlyWithADatabase() {
		// A DataSource that is never connected to: the beans only have to exist. A long interval, so the outbox
		// publisher doesn't run during the test and try to reach it
		kafkaContext.withBean(JdbcTemplate.class, () -> new JdbcTemplate(new DriverManagerDataSource()))
				.withPropertyValues("stockline.outbox.publish-interval=3600000")
				.run(context -> assertThat(context)
						.hasSingleBean(ProcessedEvents.class)
						.hasSingleBean(Outbox.class)
						.hasSingleBean(OutboxPublisher.class));
		kafkaContext.run(context -> assertThat(context)
				.doesNotHaveBean(ProcessedEvents.class)
				.doesNotHaveBean(Outbox.class)
				.doesNotHaveBean(OutboxPublisher.class));
	}

	@Test
	void kafka_nothingInAServiceWithoutSpringKafka() {
		// The gateway, auth-service and product-service don't have Spring Kafka
		kafkaContext.withClassLoader(new FilteredClassLoader(KafkaTemplate.class))
				.run(context -> assertThat(context)
						.doesNotHaveBean(EventPublisher.class)
						.doesNotHaveBean(EventCodec.class)
						.doesNotHaveBean(KafkaAdmin.NewTopics.class));
	}
}
