package com.stockline.order;

import com.jayway.jsonpath.JsonPath;
import com.stockline.order.client.ProductClient;
import com.stockline.order.client.WarehouseClient;
import com.stockline.order.dto.ProductSnapshot;
import com.stockline.order.dto.WarehouseSnapshot;
import com.stockline.order.entity.Customer;
import com.stockline.order.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * All layers with real MySQL; only product-service and inventory-service are replaced. Tests don't roll back, so
 * each uses its own customer.
 */
@SpringBootTest
// Security filters off: this class checks the layers together; SecurityRulesTest checks the roles
@AutoConfigureMockMvc(addFilters = false)
@Import(TestcontainersConfiguration.class)
class SalesOrderIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CustomerRepository customerRepository;

	@MockitoBean
	private ProductClient productClient;

	@MockitoBean
	private WarehouseClient warehouseClient;

	@Test
	void createdOrder_isPendingWithCopiesOfTheProducts_andItsFirstHistoryRow() throws Exception {
		// given
		long customerId = customerRepository.save(new Customer("SO Test Ltd", null, null)).getId();
		when(warehouseClient.getWarehouse(2L)).thenReturn(new WarehouseSnapshot(2L, "WH-01"));
		when(productClient.getProduct(6L)).thenReturn(new ProductSnapshot(6L, "A01", "Wireless Mouse", new BigDecimal("12"), "ACTIVE"));
		when(productClient.getProduct(7L)).thenReturn(new ProductSnapshot(7L, "A02", "Laptop Stand", new BigDecimal("24.00"), "ACTIVE"));

		// when
		String body = create(customerId, Map.of(6L, 5, 7L, 2))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orderNumber").value(org.hamcrest.Matchers.matchesPattern("SO-\\d{4,}")))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.customerName").value("SO Test Ltd"))
				.andExpect(jsonPath("$.warehouseCode").value("WH-01"))
				.andExpect(jsonPath("$.totalAmount").value(108.00))
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.createdBy").value("system"))
				.andReturn().getResponse().getContentAsString();
		Integer id = JsonPath.read(body, "$.id");

		// then: the price was copied, so a later change in product-service doesn't touch this order
		when(productClient.getProduct(6L)).thenReturn(new ProductSnapshot(6L, "A01", "Mouse v2", new BigDecimal("99.00"), "ACTIVE"));
		mockMvc.perform(get("/api/sales-orders/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[?(@.productId == 6)].productName").value("Wireless Mouse"))
				.andExpect(jsonPath("$.items[?(@.productId == 6)].unitPrice").value(12.00))
				.andExpect(jsonPath("$.items[?(@.productId == 6)].lineTotal").value(60.00));
		assertThat(jdbcTemplate.queryForList(
				"SELECT CONCAT(IFNULL(from_status, '-'), '>', to_status) FROM order_status_history WHERE sales_order_id = ?",
				String.class, id)).containsExactly("->PENDING");

		// The list finds it, and the customer now has an order and can't be deleted
		mockMvc.perform(get("/api/sales-orders").param("search", "so test").param("status", "PENDING"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].totalAmount").value(108.00));
		mockMvc.perform(get("/api/customers/{id}", customerId)).andExpect(jsonPath("$.orderCount").value(1));
		mockMvc.perform(delete("/api/customers/{id}", customerId)).andExpect(status().isConflict());
	}

	@Test
	void twoOrders_getConsecutiveNumbers() throws Exception {
		// given
		long customerId = customerRepository.save(new Customer("SO Numbers Ltd", null, null)).getId();
		when(warehouseClient.getWarehouse(2L)).thenReturn(new WarehouseSnapshot(2L, "WH-01"));
		when(productClient.getProduct(6L)).thenReturn(new ProductSnapshot(6L, "A01", "Wireless Mouse", new BigDecimal("12.00"), "ACTIVE"));

		// when
		String first = JsonPath.read(create(customerId, Map.of(6L, 1)).andReturn().getResponse().getContentAsString(), "$.orderNumber");
		String second = JsonPath.read(create(customerId, Map.of(6L, 1)).andReturn().getResponse().getContentAsString(), "$.orderNumber");

		// then
		assertThat(Long.parseLong(second.substring(3))).isEqualTo(Long.parseLong(first.substring(3)) + 1);
	}

	@Test
	void refusedOrder_savesNothing_andUsesNoNumber() throws Exception {
		// given
		long customerId = customerRepository.save(new Customer("SO Refused Ltd", null, null)).getId();
		when(warehouseClient.getWarehouse(2L)).thenReturn(new WarehouseSnapshot(2L, "WH-01"));
		when(productClient.getProduct(8L)).thenReturn(new ProductSnapshot(8L, "A03", "Old Keyboard", new BigDecimal("30.00"), "INACTIVE"));
		long nextBefore = nextNumber();

		// when / then
		create(customerId, Map.of(8L, 1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Product 'A03' is inactive and can not be sold"));
		create(999_999, Map.of(8L, 1)).andExpect(status().isNotFound());
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sales_orders WHERE customer_id = ?", Long.class,
				customerId)).isZero();
		assertThat(nextNumber()).isEqualTo(nextBefore);
	}

	private long nextNumber() {
		return jdbcTemplate.queryForObject("SELECT next_value FROM number_sequences WHERE name = 'sales_order'", Long.class);
	}

	private ResultActions create(long customerId, Map<Long, Integer> quantities) throws Exception {
		StringBuilder items = new StringBuilder();
		quantities.forEach((productId, quantity) -> items.append(items.isEmpty() ? "" : ", ")
				.append("{ \"productId\": ").append(productId).append(", \"quantity\": ").append(quantity).append(" }"));
		return mockMvc.perform(post("/api/sales-orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "customerId": %d, "warehouseId": 2, "items": [ %s ] }
						""".formatted(customerId, items)));
	}
}
