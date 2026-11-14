package com.stockline.common.autoconfigure;

import com.stockline.common.event.EventCodec;
import com.stockline.common.event.EventPublisher;
import com.stockline.common.event.KafkaTopics;
import com.stockline.common.event.Outbox;
import com.stockline.common.event.OutboxPublisher;
import com.stockline.common.event.ProcessedEvents;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

/**
 * Kafka for services that have Spring Kafka on the classpath (order-service and inventory-service); the others get
 * none of it. After Boot's own Kafka and Jackson auto-configurations, so their KafkaTemplate and JsonMapper exist.
 * Class names as strings: a service without Spring Kafka must still be able to read this annotation.
 */
@AutoConfiguration(
		after = CommonTimeAutoConfiguration.class,
		afterName = {
				"org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration",
				"org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
				"org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration"})
@ConditionalOnClass(KafkaTemplate.class)
public class CommonKafkaAutoConfiguration {

	/** The service's JsonMapper, so events format dates like the API does; a plain one if the service has none. */
	@Bean
	@ConditionalOnMissingBean
	EventCodec eventCodec(ObjectProvider<JsonMapper> jsonMapper) {
		return new EventCodec(jsonMapper.getIfAvailable(() -> JsonMapper.builder().build()));
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(KafkaTemplate.class)
	EventPublisher eventPublisher(KafkaTemplate<String, String> kafkaTemplate, EventCodec eventCodec, Clock clock) {
		return new EventPublisher(kafkaTemplate, eventCodec, clock);
	}

	/**
	 * Every topic, in every service that uses Kafka: Spring's {@link KafkaAdmin} creates the missing ones when the
	 * service starts and leaves existing ones alone. So no service depends on another one starting first.
	 */
	@Bean
	KafkaAdmin.NewTopics stocklineTopics() {
		return new KafkaAdmin.NewTopics(topics());
	}

	/**
	 * What needs the service's database: idempotent consumers ({@link ProcessedEvents}) and the outbox ({@link Outbox},
	 * {@link OutboxPublisher}). A nested class, so its condition on JdbcTemplate is checked only when Spring JDBC is on
	 * the classpath. {@code @EnableScheduling} switches on {@code @Scheduled}, which runs the outbox publisher.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JdbcTemplate.class)
	@EnableScheduling
	static class DatabaseEventsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(JdbcTemplate.class)
		ProcessedEvents processedEvents(JdbcTemplate jdbcTemplate, Clock clock, MessageSource messageSource) {
			return new ProcessedEvents(jdbcTemplate, clock, messageSource);
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(JdbcTemplate.class)
		Outbox outbox(JdbcTemplate jdbcTemplate, EventCodec eventCodec, Clock clock) {
			return new Outbox(jdbcTemplate, eventCodec, clock);
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean({JdbcTemplate.class, KafkaTemplate.class})
		OutboxPublisher outboxPublisher(JdbcTemplate jdbcTemplate, KafkaTemplate<String, String> kafkaTemplate, Clock clock,
										MessageSource messageSource) {
			return new OutboxPublisher(jdbcTemplate, kafkaTemplate, clock, messageSource);
		}
	}

	/** One NewTopic per name in {@link KafkaTopics#ALL}. Package-private for the test. */
	static NewTopic[] topics() {
		return KafkaTopics.ALL.stream()
				.map(name -> TopicBuilder.name(name).partitions(KafkaTopics.PARTITIONS).replicas(KafkaTopics.REPLICAS).build())
				.toArray(NewTopic[]::new);
	}
}
