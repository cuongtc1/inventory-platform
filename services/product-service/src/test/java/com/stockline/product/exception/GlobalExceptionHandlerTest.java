package com.stockline.product.exception;

import com.stockline.product.config.TimeConfig;
import com.stockline.product.controller.ProductController;
import com.stockline.product.dto.ProductRequest;
import com.stockline.product.service.ProductService;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.SQLException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Error responses that don't belong to one controller: broken JSON, wrong types, Spring MVC errors,
 * unexpected exceptions. ProductController is used as the example endpoint; its service is a mock.
 * Every response must have the full convention format (type, title, status, detail, instance, code, timestamp).
 */
@WebMvcTest(ProductController.class)
@Import(TimeConfig.class)
class GlobalExceptionHandlerTest {

	private static final String VALID_BODY = """
			{ "sku": "A01", "name": "Wireless Mouse", "categoryId": 3,
			  "costPrice": 8.00, "salePrice": 12.00, "reorderLevel": 20, "status": "ACTIVE" }
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductService productService;

	@Test
	void brokenJson_returns400BadRequest() throws Exception {
		// when
		ResultActions result = mockMvc.perform(post("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"sku\": \"A01\", "));

		// then
		expectStandardError(result, 400, "BAD_REQUEST", "/api/products")
				.andExpect(jsonPath("$.detail").value("Request body is missing or is not valid JSON"));
		verifyNoInteractions(productService);
	}

	@Test
	void missingBody_returns400BadRequest() throws Exception {
		// when
		ResultActions result = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON));

		// then
		expectStandardError(result, 400, "BAD_REQUEST", "/api/products");
	}

	@Test
	void unknownEnumValueInBody_returns400ValidationFailedForThatField() throws Exception {
		// when
		ResultActions result = mockMvc.perform(post("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_BODY.replace("\"ACTIVE\"", "\"SOLD_OUT\"")));

		// then
		expectStandardError(result, 400, "VALIDATION_FAILED", "/api/products")
				.andExpect(jsonPath("$.errors[0].field").value("status"))
				.andExpect(jsonPath("$.errors[0].message").value("has an invalid value 'SOLD_OUT'"));
	}

	@Test
	void textInNumberField_returns400ValidationFailedForThatField() throws Exception {
		// when
		ResultActions result = mockMvc.perform(post("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_BODY.replace("\"categoryId\": 3", "\"categoryId\": \"abc\"")));

		// then
		expectStandardError(result, 400, "VALIDATION_FAILED", "/api/products")
				.andExpect(jsonPath("$.errors[0].field").value("categoryId"))
				.andExpect(jsonPath("$.errors[0].message").value("has an invalid value 'abc'"));
	}

	@Test
	void objectInNumberField_returns400ValidationFailedWithWrongType() throws Exception {
		// when
		ResultActions result = mockMvc.perform(post("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_BODY.replace("\"reorderLevel\": 20", "\"reorderLevel\": { \"min\": 20 }")));

		// then
		expectStandardError(result, 400, "VALIDATION_FAILED", "/api/products")
				.andExpect(jsonPath("$.errors[0].field").value("reorderLevel"))
				.andExpect(jsonPath("$.errors[0].message").value("has a value of the wrong type"));
	}

	@Test
	void textAsPathId_returns400BadRequest() throws Exception {
		// when
		ResultActions result = mockMvc.perform(get("/api/products/abc"));

		// then
		expectStandardError(result, 400, "BAD_REQUEST", "/api/products/abc")
				.andExpect(jsonPath("$.detail").value("Parameter 'id' has an invalid value 'abc'"));
	}

	@Test
	void unsupportedMethod_returns405MethodNotAllowed() throws Exception {
		// when: we use PUT, not PATCH
		ResultActions result = mockMvc.perform(patch("/api/products/1"));

		// then
		expectStandardError(result, 405, "METHOD_NOT_ALLOWED", "/api/products/1");
	}

	@Test
	void notJsonContentType_returns415UnsupportedMediaType() throws Exception {
		// when
		ResultActions result = mockMvc.perform(post("/api/products")
				.contentType(MediaType.TEXT_PLAIN)
				.content("sku=A01"));

		// then
		expectStandardError(result, 415, "UNSUPPORTED_MEDIA_TYPE", "/api/products");
	}

	@Test
	void unknownUrl_returns404NotFound() throws Exception {
		// when
		ResultActions result = mockMvc.perform(get("/api/unknown"));

		// then
		expectStandardError(result, 404, "NOT_FOUND", "/api/unknown")
				.andExpect(jsonPath("$.detail").value("No endpoint GET /api/unknown"));
	}

	@Test
	void unexpectedException_returns500WithoutInternalDetails() throws Exception {
		// given
		when(productService.get(1L)).thenThrow(new IllegalStateException("connection pool exhausted"));

		// when
		ResultActions result = mockMvc.perform(get("/api/products/1"));

		// then: the client sees a generic message; the real one is only in the log
		expectStandardError(result, 500, "INTERNAL_ERROR", "/api/products/1")
				.andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
				.andExpect(content().string(not(containsString("connection pool"))));
	}

	@Test
	void uniqueKeyViolatedByConcurrentRequest_returns409Duplicate() throws Exception {
		// given: both requests passed existsBySku(), MySQL rejected the second insert
		when(productService.create(any(ProductRequest.class))).thenThrow(new DataIntegrityViolationException(
				"could not execute statement",
				new ConstraintViolationException("Duplicate entry 'A01' for key 'products.uk_products_sku'",
						new SQLException("Duplicate entry", "23000", 1062), ConstraintKind.UNIQUE,
						"products.uk_products_sku")));

		// when
		ResultActions result = mockMvc.perform(post("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_BODY));

		// then
		expectStandardError(result, 409, "DUPLICATE", "/api/products")
				.andExpect(jsonPath("$.detail").value("A product with this SKU already exists"));
	}

	@Test
	void dataIntegrityViolationWithoutConstraint_returns500() throws Exception {
		// given: not a constraint error we know how to explain
		when(productService.get(1L)).thenThrow(new DataIntegrityViolationException("Data too long for column"));

		// when / then
		expectStandardError(mockMvc.perform(get("/api/products/1")), 500, "INTERNAL_ERROR", "/api/products/1");
	}

	private static ResultActions expectStandardError(ResultActions result, int status, String code, String path)
			throws Exception {
		return result
				.andExpect(status().is(status))
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("about:blank"))
				.andExpect(jsonPath("$.title").isNotEmpty())
				.andExpect(jsonPath("$.status").value(status))
				.andExpect(jsonPath("$.detail").isNotEmpty())
				.andExpect(jsonPath("$.instance").value(path))
				.andExpect(jsonPath("$.code").value(code))
				.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}
}
