package com.stockline.order;

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
 * All layers with real MySQL. Tests don't roll back, so each uses its own names.
 */
@SpringBootTest
// Security filters off: this class checks the layers together; SecurityRulesTest checks the roles
@AutoConfigureMockMvc(addFilters = false)
@Import(TestcontainersConfiguration.class)
class CustomerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void customerLifecycle() throws Exception {
		// Create
		String created = mockMvc.perform(post("/api/customers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": " IT Customer ", "email": "", "phone": "  " }
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("IT Customer"))
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.phone").doesNotExist())
				.andExpect(jsonPath("$.orderCount").value(0))
				.andExpect(jsonPath("$.createdBy").value("system"))
				.andReturn().getResponse().getContentAsString();
		Integer id = JsonPath.read(created, "$.id");

		// The same name in other capitals is taken
		mockMvc.perform(post("/api/customers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "it customer" }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE"));

		// Update
		mockMvc.perform(put("/api/customers/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "IT Customer Ltd", "email": "it@customer.test", "phone": "+84 90 111 2233" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("IT Customer Ltd"))
				.andExpect(jsonPath("$.email").value("it@customer.test"));

		// With an order it shows in the count, and the customer can't be deleted
		jdbcTemplate.update("""
				INSERT INTO sales_orders (order_number, customer_id, warehouse_id, warehouse_code, status, total_amount,
				                          created_at, updated_at, created_by, updated_by)
				VALUES ('SO-IT1', ?, 1, 'WH-01', 'PENDING', 60.00, NOW(6), NOW(6), 'test', 'test')
				""", id);
		mockMvc.perform(get("/api/customers").param("search", "it@customer"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].orderCount").value(1));
		mockMvc.perform(get("/api/customers/{id}", id)).andExpect(jsonPath("$.orderCount").value(1));
		mockMvc.perform(delete("/api/customers/{id}", id))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("IN_USE"));

		// Without orders it can
		jdbcTemplate.update("DELETE FROM sales_orders WHERE customer_id = ?", id);
		mockMvc.perform(delete("/api/customers/{id}", id)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/customers/{id}", id)).andExpect(status().isNotFound());
	}

	@Test
	void unknownSortField_is400() throws Exception {
		mockMvc.perform(get("/api/customers").param("sort", "color,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}
}
