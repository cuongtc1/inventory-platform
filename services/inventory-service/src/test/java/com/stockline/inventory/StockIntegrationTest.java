package com.stockline.inventory;

import com.jayway.jsonpath.JsonPath;
import com.stockline.common.util.DateTimeUtils;
import com.stockline.inventory.client.ProductClient;
import com.stockline.inventory.dto.ProductSnapshot;
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

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * All layers with real MySQL; only product-service is replaced. Tests don't roll back, so each uses its own products
 * and warehouses.
 */
@SpringBootTest
// Security filters off: this class checks the layers together; SecurityRulesTest checks the roles
@AutoConfigureMockMvc(addFilters = false)
@Import(TestcontainersConfiguration.class)
class StockIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Clock clock;

	@MockitoBean
	private ProductClient productClient;

	@Test
	void adjustmentsLifecycle() throws Exception {
		// given
		long warehouseId = createWarehouse("ST-01");
		when(productClient.getProduct(101L)).thenReturn(new ProductSnapshot(101L, "ST-A", "Stock Mouse", 20));

		// The first adjustment creates the stock level, with the copies from product-service
		adjust(101, warehouseId, 50, "Opening")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.onHand").value(50))
				.andExpect(jsonPath("$.available").value(50))
				.andExpect(jsonPath("$.productName").value("Stock Mouse"))
				.andExpect(jsonPath("$.warehouseCode").value("ST-01"))
				.andExpect(jsonPath("$.lowStock").value(false));

		// The next one changes it, and refreshes the copies: the product was renamed meanwhile
		when(productClient.getProduct(101L)).thenReturn(new ProductSnapshot(101L, "ST-A", "Stock Mouse 2", 40));
		adjust(101, warehouseId, -10, null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.onHand").value(40))
				.andExpect(jsonPath("$.productName").value("Stock Mouse 2"))
				.andExpect(jsonPath("$.lowStock").value(true));

		// Reserved stock can't be taken away (a reservation comes with Phase 6, so it is set by hand here)
		jdbcTemplate.update("UPDATE stock_levels SET reserved = 30 WHERE product_id = 101");
		adjust(101, warehouseId, -11, null)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
				.andExpect(jsonPath("$.detail").value("Not enough stock of 'Stock Mouse 2' in ST-01: only 10 available"));
		adjust(101, warehouseId, -10, null).andExpect(status().isOk()).andExpect(jsonPath("$.available").value(0));

		// One movement per successful adjustment, none for the refused one
		assertThat(jdbcTemplate.queryForList(
				"SELECT quantity FROM stock_movements WHERE product_id = 101 ORDER BY id", Integer.class))
				.containsExactly(50, -10, -10);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT reference FROM stock_movements WHERE product_id = 101 AND quantity = 50", String.class))
				.isEqualTo("Opening");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT movement_type FROM stock_movements WHERE product_id = 101 AND quantity = 50", String.class))
				.isEqualTo("ADJUST");

		// The list finds it with the low-stock filter, and the warehouse counts it
		mockMvc.perform(get("/api/stock-levels").param("search", "stock mouse").param("lowStock", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].reserved").value(30));
		mockMvc.perform(get("/api/warehouses/{id}", warehouseId)).andExpect(jsonPath("$.skuCount").value(1));

		// The history, newest first, with the product name as it was at each moment
		mockMvc.perform(get("/api/stock-movements").param("productId", "101").param("movementType", "ADJUST"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.content[2].quantity").value(50))
				.andExpect(jsonPath("$.content[2].productName").value("Stock Mouse"))
				.andExpect(jsonPath("$.content[2].reference").value("Opening"))
				.andExpect(jsonPath("$.content[2].warehouseCode").value("ST-01"))
				.andExpect(jsonPath("$.content[0].productName").value("Stock Mouse 2"))
				.andExpect(jsonPath("$.content[0].createdBy").value("system"));

		// Today in Vietnam, from the same clock as the audit columns
		String today = DateTimeUtils.today(clock).toString();
		mockMvc.perform(get("/api/stock-movements").param("productId", "101").param("from", today).param("to", today))
				.andExpect(jsonPath("$.totalElements").value(3));
		mockMvc.perform(get("/api/stock-movements").param("productId", "101").param("to", "2020-01-01"))
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void adjust_refusals() throws Exception {
		// given
		long warehouseId = createWarehouse("ST-02");
		when(productClient.getProduct(102L)).thenReturn(new ProductSnapshot(102L, "ST-B", "Stock Cable", 0));

		// Zero, an unknown warehouse, and taking from nothing
		adjust(102, warehouseId, 0, null)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
		adjust(102, 999_999, 5, null).andExpect(status().isNotFound());
		adjust(102, warehouseId, -1, null).andExpect(status().isConflict());

		// Nothing was stored
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stock_levels WHERE product_id = 102", Long.class))
				.isZero();
	}

	private long createWarehouse(String code) throws Exception {
		String body = mockMvc.perform(post("/api/warehouses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "code": "%s", "name": "Stock test" }
								""".formatted(code)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private ResultActions adjust(long productId, long warehouseId, int quantity, String reference) throws Exception {
		String referenceJson = reference == null ? "null" : "\"" + reference + "\"";
		return mockMvc.perform(post("/api/stock-levels/adjust")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "productId": %d, "warehouseId": %d, "quantity": %d, "reference": %s }
						""".formatted(productId, warehouseId, quantity, referenceJson)));
	}
}
