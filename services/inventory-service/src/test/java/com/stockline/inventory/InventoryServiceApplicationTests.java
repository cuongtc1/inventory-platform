package com.stockline.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Starts the whole application against a Testcontainers MySQL.
 * Passes when every bean is created, Flyway runs and Hibernate validates the schema.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class InventoryServiceApplicationTests {

	@Test
	void contextLoads() {
	}
}
