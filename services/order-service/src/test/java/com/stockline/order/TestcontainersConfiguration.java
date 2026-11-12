package com.stockline.order;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a throwaway MySQL container for tests that need a real database.
 * Import it with {@code @Import(TestcontainersConfiguration.class)}.
 *
 * <p>{@code @ServiceConnection} tells Spring Boot to point the datasource at this container,
 * so the URL, username and password in application.yml are replaced for the test run.
 * Docker must be running; the container is removed when the tests finish.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		// Same image as infra/docker-compose.yml, so tests run on the same MySQL version
		return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
	}

	/**
	 * One Kafka broker for the whole test run, shared by every Spring context: a static field instead of a new
	 * container per context, because a broker is heavier than MySQL. The same image as infra/docker-compose.yml, with a
	 * small heap. Tests share its topics, so each test reads only the messages it sent itself.
	 */
	private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.0"))
			.withEnv("KAFKA_HEAP_OPTS", "-Xms256m -Xmx256m");

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		return KAFKA;
	}
}
