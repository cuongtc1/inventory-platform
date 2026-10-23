package com.stockline.product;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole application with a real MySQL (Testcontainers): HTTP request → controller → service → repository →
 * Flyway tables, and back to JSON. Nothing is mocked. The unit and slice tests check each layer alone; this
 * checks that they work together, e.g. that transactions, lazy loading and the entity graph behave.
 *
 * <p>Tests don't roll back (that would hide commit-time errors), so each test uses its own names and SKUs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductCatalogIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void catalogLifecycle_createListUpdateDelete() throws Exception {
		// Categories: a parent and a child
		long electronics = create("/api/categories", """
				{ "name": "IT Electronics" }
				""");
		long cables = create("/api/categories", """
				{ "name": "IT Cables", "parentId": %d }
				""".formatted(electronics));
		mockMvc.perform(get("/api/categories/{id}", cables))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.parentName").value("IT Electronics"));

		// A supplier and two products
		long wireCo = create("/api/suppliers", """
				{ "name": "IT WireCo", "email": "tom@wireco.com" }
				""");
		long usbCable = create("/api/products", product("IT-A02", "IT USB-C Cable", cables, wireCo, "3.50"));
		create("/api/products", product("IT-A05", "IT HDMI Adapter", cables, null, "7.90"));

		// List with filters, sorted by price
		mockMvc.perform(get("/api/products")
						.param("search", "it-a0")
						.param("categoryId", String.valueOf(cables))
						.param("sort", "salePrice,desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[*].sku", contains("IT-A05", "IT-A02")))
				.andExpect(jsonPath("$.content[1].supplierName").value("IT WireCo"))
				.andExpect(jsonPath("$.content[1].categoryName").value("IT Cables"));
		mockMvc.perform(get("/api/products").param("supplierId", String.valueOf(wireCo)))
				.andExpect(jsonPath("$.content", hasSize(1)));

		// Update: price is stored and returned with 2 decimals
		mockMvc.perform(put("/api/products/{id}", usbCable)
						.contentType(MediaType.APPLICATION_JSON)
						.content(product("IT-A02", "IT USB-C Cable 2m", cables, wireCo, "4")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("IT USB-C Cable 2m"));
		mockMvc.perform(get("/api/products/{id}", usbCable))
				.andExpect(jsonPath("$.salePrice").value(4.00))
				.andExpect(rawJson("salePrice", "4.00"));

		// Deleting a category or supplier that products still use is refused
		mockMvc.perform(delete("/api/categories/{id}", cables))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("IN_USE"))
				.andExpect(jsonPath("$.detail").value("Category 'IT Cables' still has products; move or delete them first"));
		mockMvc.perform(delete("/api/categories/{id}", electronics))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Category 'IT Electronics' has subcategories; move or delete them first"));
		mockMvc.perform(delete("/api/suppliers/{id}", wireCo))
				.andExpect(status().isConflict());

		// Delete the product; now the supplier can go
		mockMvc.perform(delete("/api/products/{id}", usbCable)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/products/{id}", usbCable))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Product with id " + usbCable + " not found"));
		mockMvc.perform(delete("/api/suppliers/{id}", wireCo)).andExpect(status().isNoContent());
	}

	@Test
	void duplicates_return409WithTextFromMessagesProperties() throws Exception {
		// given
		long category = create("/api/categories", """
				{ "name": "IT Duplicates" }
				""");
		create("/api/products", product("IT-DUP-1", "IT First", category, null, "1.00"));

		// then: same SKU in other case, same category name in other case
		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content(product("it-dup-1", "IT Second", category, null, "1.00")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE"))
				.andExpect(jsonPath("$.detail").value("Product with SKU 'it-dup-1' already exists"));
		mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "it duplicates" }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Category 'it duplicates' already exists"));
	}

	@Test
	void validation_returnsFieldMessagesFromMessagesProperties() throws Exception {
		mockMvc.perform(post("/api/suppliers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "", "phone": "call me" }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors[?(@.field == 'name')].message").value("must not be blank"))
				.andExpect(jsonPath("$.errors[?(@.field == 'phone')].message")
						.value("may contain only digits, spaces, +, -, ( and )"));
	}

	@Test
	void productWithUnknownCategory_returns404() throws Exception {
		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content(product("IT-NOCAT", "IT No category", 999_999L, null, "1.00")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Category with id 999999 not found"));
	}

	/**
	 * POSTs the body, checks 201 and the Location header, and returns the new id.
	 */
	private long create(String url, String body) throws Exception {
		MvcResult result = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn();
		long id = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
		mockMvc.perform(get(url + "/" + id)).andExpect(status().isOk());
		header().string("Location", url + "/" + id).match(result);
		return id;
	}

	private static String product(String sku, String name, long categoryId, Long supplierId, String salePrice) {
		return """
				{ "sku": "%s", "name": "%s", "categoryId": %d, "supplierId": %s,
				  "costPrice": 1.00, "salePrice": %s, "reorderLevel": 10, "status": "ACTIVE" }
				""".formatted(sku, name, categoryId, supplierId, salePrice);
	}

	/**
	 * Checks the raw JSON text, e.g. that 4.00 is written as {@code "salePrice":4.00} and not 4 or 4.0.
	 * jsonPath() can't tell them apart because it compares numbers by value.
	 */
	private static ResultMatcher rawJson(String field, String rawNumber) {
		return result -> assertThat(result.getResponse().getContentAsString())
				.contains("\"" + field + "\":" + rawNumber);
	}
}
