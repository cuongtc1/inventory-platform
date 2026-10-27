package com.stockline.product.controller;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.DuplicateResourceException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.product.constant.MessageKeys;
import com.stockline.product.constant.ProductStatus;
import com.stockline.product.dto.ProductFilter;
import com.stockline.product.dto.ProductRequest;
import com.stockline.product.dto.ProductResponse;
import com.stockline.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer only; the service is a Mockito mock. Checks URLs, filter binding, status codes, JSON and validation.
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

	private static final Instant NOW = Instant.parse("2026-10-23T08:00:00Z");

	private static final ProductResponse MOUSE = new ProductResponse(12L, "A01", "Wireless Mouse",
			3L, "Peripherals", 5L, "LogiCo", new BigDecimal("8.00"), new BigDecimal("12.00"), 20,
			ProductStatus.ACTIVE, NOW, "system", NOW, "system");

	private static final String VALID_BODY = """
			{ "sku": "A01", "name": "Wireless Mouse", "categoryId": 3, "supplierId": 5,
			  "costPrice": 8.00, "salePrice": 12.00, "reorderLevel": 20, "status": "ACTIVE" }
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductService productService;

	@Test
	void list_noParameters_usesNoFiltersAndDefaultPage() throws Exception {
		// given
		when(productService.list(any(ProductFilter.class), any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(MOUSE), 0, 20, 1, 1));

		// when / then
		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].sku").value("A01"))
				.andExpect(jsonPath("$.content[0].categoryName").value("Peripherals"))
				.andExpect(jsonPath("$.content[0].supplierName").value("LogiCo"))
				.andExpect(jsonPath("$.content[0].salePrice").value(12.00))
				.andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
				.andExpect(jsonPath("$.content[0].createdBy").value("system"))
				.andExpect(jsonPath("$.content[0].updatedBy").value("system"))
				.andExpect(jsonPath("$.totalElements").value(1));
		verify(productService).list(
				eq(new ProductFilter(null, null, null, null)),
				argThat(pageable -> pageable.getPageNumber() == 0
						&& pageable.getPageSize() == 20
						&& pageable.getSort().equals(Sort.by("name"))));
	}

	@Test
	void list_allFilters_bindsThemIntoProductFilter() throws Exception {
		// given
		when(productService.list(any(ProductFilter.class), any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(), 1, 5, 0, 0));

		// when / then
		mockMvc.perform(get("/api/products")
						.param("search", "mouse")
						.param("categoryId", "3")
						.param("supplierId", "5")
						.param("status", "INACTIVE")
						.param("page", "1")
						.param("size", "5")
						.param("sort", "salePrice,desc"))
				.andExpect(status().isOk());
		verify(productService).list(
				eq(new ProductFilter("mouse", 3L, 5L, ProductStatus.INACTIVE)),
				argThat(pageable -> pageable.getPageNumber() == 1
						&& pageable.getPageSize() == 5
						&& pageable.getSort().equals(Sort.by(Sort.Direction.DESC, "salePrice"))));
	}

	@Test
	void list_unknownStatus_returns400WithFieldError() throws Exception {
		// when / then
		mockMvc.perform(get("/api/products").param("status", "WRONG"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors[0].field").value("status"))
				.andExpect(jsonPath("$.errors[0].message").value("has an invalid value 'WRONG'"));
		verifyNoInteractions(productService);
	}

	@Test
	void get_existingId_returns200WithProduct() throws Exception {
		// given
		when(productService.get(12L)).thenReturn(MOUSE);

		// when / then
		mockMvc.perform(get("/api/products/12"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(12))
				.andExpect(jsonPath("$.sku").value("A01"))
				.andExpect(jsonPath("$.categoryName").value("Peripherals"))
				.andExpect(jsonPath("$.costPrice").value(8.00))
				.andExpect(jsonPath("$.reorderLevel").value(20));
	}

	@Test
	void update_validRequest_returns200WithUpdatedProduct() throws Exception {
		// given
		when(productService.update(eq(12L), any(ProductRequest.class))).thenReturn(MOUSE);

		// when / then
		mockMvc.perform(put("/api/products/12").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(12));
		verify(productService).update(12L, new ProductRequest("A01", "Wireless Mouse", 3L, 5L,
				new BigDecimal("8.00"), new BigDecimal("12.00"), 20, ProductStatus.ACTIVE));
	}

	@Test
	void get_unknownId_returns404ProblemJson() throws Exception {
		// given
		when(productService.get(99L)).thenThrow(new ResourceNotFoundException("Product", 99L));

		// when / then
		mockMvc.perform(get("/api/products/99"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Product with id 99 not found"))
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void create_validRequest_returns201WithLocation() throws Exception {
		// given
		when(productService.create(any(ProductRequest.class))).thenReturn(MOUSE);

		// when / then
		mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/products/12"))
				.andExpect(jsonPath("$.id").value(12));
		verify(productService).create(new ProductRequest("A01", "Wireless Mouse", 3L, 5L,
				new BigDecimal("8.00"), new BigDecimal("12.00"), 20, ProductStatus.ACTIVE));
	}

	@Test
	void create_invalidFields_returns400WithFieldErrors() throws Exception {
		// when / then
		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "sku": "A 01", "name": "", "costPrice": -1, "salePrice": 12.345,
								  "reorderLevel": -5 }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors.length()").value(7))
				.andExpect(jsonPath("$.errors[?(@.field == 'sku')].message")
						.value("may contain only letters, digits, '.', '_' and '-'"))
				.andExpect(jsonPath("$.errors[?(@.field == 'name')].message").value("must not be blank"))
				.andExpect(jsonPath("$.errors[?(@.field == 'categoryId')].message").value("must not be null"))
				.andExpect(jsonPath("$.errors[?(@.field == 'costPrice')].message")
						.value("must be greater than or equal to 0"))
				.andExpect(jsonPath("$.errors[?(@.field == 'salePrice')].message")
						.value("numeric value out of bounds (<10 digits>.<2 digits> expected)"))
				.andExpect(jsonPath("$.errors[?(@.field == 'reorderLevel')].message")
						.value("must be greater than or equal to 0"))
				.andExpect(jsonPath("$.errors[?(@.field == 'status')].message").value("must not be null"));
		verifyNoInteractions(productService);
	}

	@Test
	void update_duplicateSku_returns409Duplicate() throws Exception {
		// given
		when(productService.update(eq(12L), any(ProductRequest.class)))
				.thenThrow(new DuplicateResourceException(MessageKeys.PRODUCT_SKU_DUPLICATE, "A01"));

		// when / then
		mockMvc.perform(put("/api/products/12").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE"))
				.andExpect(jsonPath("$.detail").value("Product with SKU 'A01' already exists"));
	}

	@Test
	void delete_existingProduct_returns204() throws Exception {
		// when / then
		mockMvc.perform(delete("/api/products/12"))
				.andExpect(status().isNoContent());
		verify(productService).delete(12L);
	}
}
