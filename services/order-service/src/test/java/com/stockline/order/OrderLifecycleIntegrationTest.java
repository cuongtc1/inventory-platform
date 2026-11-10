package com.stockline.order;

import com.jayway.jsonpath.JsonPath;
import com.stockline.order.client.ProductClient;
import com.stockline.order.client.WarehouseClient;
import com.stockline.order.constant.OrderStatus;
import com.stockline.order.dto.ProductSnapshot;
import com.stockline.order.dto.WarehouseSnapshot;
import com.stockline.order.entity.Customer;
import com.stockline.order.repository.CustomerRepository;
import com.stockline.order.service.OrderStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An order through its whole life on real MySQL. The saga of Phase 6 isn't there yet, so the reservation is done by
 * calling OrderStatusService directly, as the Kafka consumer will.
 */
@SpringBootTest
// Security filters off: this class checks the layers together; SecurityRulesTest checks the roles
@AutoConfigureMockMvc(addFilters = false)
@Import(TestcontainersConfiguration.class)
class OrderLifecycleIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private OrderStatusService orderStatusService;

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
	void happyPath_recordsEveryStep() throws Exception {
		// given
		long id = createOrder("Lifecycle Happy Ltd");
		orderStatusService.changeStatus(id, OrderStatus.RESERVED, null);

		// when
		action(id, "pay").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
		action(id, "ship").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SHIPPED"));
		action(id, "complete").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));

		// then: one history row per change, oldest first
		mockMvc.perform(get("/api/sales-orders/{id}/history", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(5))
				.andExpect(jsonPath("$[0].fromStatus").doesNotExist())
				.andExpect(jsonPath("$[0].toStatus").value("PENDING"))
				.andExpect(jsonPath("$[1].toStatus").value("RESERVED"))
				.andExpect(jsonPath("$[4].fromStatus").value("SHIPPED"))
				.andExpect(jsonPath("$[4].toStatus").value("COMPLETED"))
				.andExpect(jsonPath("$[4].createdBy").value("system"));

		// Nothing after COMPLETED
		action(id, "cancel")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
	}

	@Test
	void wrongOrder_is409_andRecordsNothing() throws Exception {
		// given
		long id = createOrder("Lifecycle Wrong Ltd");

		// when / then: a PENDING order can't be paid, shipped or completed
		action(id, "pay")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"))
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.matchesPattern(
						"Order SO-\\d+ is PENDING, so it can not change to PAID")));
		action(id, "ship").andExpect(status().isConflict());
		action(id, "complete").andExpect(status().isConflict());
		mockMvc.perform(get("/api/sales-orders/{id}/history", id)).andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void cancel_beforeShipping_onlyOnce() throws Exception {
		// given
		long id = createOrder("Lifecycle Cancel Ltd");

		// when / then
		action(id, "cancel").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
		action(id, "cancel").andExpect(status().isConflict());
		mockMvc.perform(get("/api/sales-orders").param("status", "CANCELLED").param("search", "Lifecycle Cancel"))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void cancel_afterShipping_isRefused() throws Exception {
		// given
		long id = createOrder("Lifecycle Shipped Ltd");
		orderStatusService.changeStatus(id, OrderStatus.RESERVED, null);
		action(id, "pay");
		action(id, "ship");

		// when / then
		action(id, "cancel")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.endsWith("is SHIPPED, so it can not change to CANCELLED")));
	}

	@Test
	void unknownOrder_is404() throws Exception {
		action(999_999, "pay").andExpect(status().isNotFound());
		mockMvc.perform(get("/api/sales-orders/{id}/history", 999_999)).andExpect(status().isNotFound());
	}

	private long createOrder(String customerName) throws Exception {
		long customerId = customerRepository.save(new Customer(customerName, null, null)).getId();
		String body = mockMvc.perform(post("/api/sales-orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "customerId": %d, "warehouseId": 2, "items": [ { "productId": 6, "quantity": 1 } ] }
								""".formatted(customerId)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private org.springframework.test.web.servlet.ResultActions action(long id, String verb) throws Exception {
		return mockMvc.perform(post("/api/sales-orders/{id}/{verb}", id, verb));
	}
}
