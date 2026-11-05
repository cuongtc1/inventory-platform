package com.stockline.inventory;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * All layers with real MySQL. Tests don't roll back, so each uses its own codes.
 */
@SpringBootTest
// Security filters off: this class checks the layers together; SecurityRulesTest checks the roles
@AutoConfigureMockMvc(addFilters = false)
@Import(TestcontainersConfiguration.class)
class WarehouseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void warehouseLifecycle() throws Exception {
		// Create
		String created = mockMvc.perform(post("/api/warehouses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "code": "IT-01", "name": "IT Main", "address": "  " }
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.address").doesNotExist())
				.andExpect(jsonPath("$.skuCount").value(0))
				.andExpect(jsonPath("$.createdBy").value("system"))
				.andReturn().getResponse().getContentAsString();
		Integer id = JsonPath.read(created, "$.id");

		// The same code in other capitals is taken
		mockMvc.perform(post("/api/warehouses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "code": "it-01", "name": "Copy" }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE"));

		// Update
		mockMvc.perform(put("/api/warehouses/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "code": "IT-01", "name": "IT Main Warehouse", "address": "District 7" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("IT Main Warehouse"))
				.andExpect(jsonPath("$.address").value("District 7"));

		// With a stock level it shows in the count, and the warehouse can't be deleted
		jdbcTemplate.update("""
				INSERT INTO stock_levels (warehouse_id, product_id, product_sku, product_name,
				                          created_at, updated_at, created_by, updated_by)
				VALUES (?, 12, 'A01', 'Mouse', NOW(6), NOW(6), 'test', 'test')
				""", id);
		mockMvc.perform(get("/api/warehouses").param("search", "it-0"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].skuCount").value(1));
		mockMvc.perform(delete("/api/warehouses/{id}", id))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("IN_USE"));

		// Without stock it can
		jdbcTemplate.update("DELETE FROM stock_levels WHERE warehouse_id = ?", id);
		mockMvc.perform(delete("/api/warehouses/{id}", id)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/warehouses/{id}", id)).andExpect(status().isNotFound());
	}

	@Test
	void unknownSortField_is400() throws Exception {
		mockMvc.perform(get("/api/warehouses").param("sort", "color,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}
}
