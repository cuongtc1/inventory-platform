package com.stockline.product.controller;

import com.stockline.product.config.TimeConfig;
import com.stockline.product.constant.MessageKeys;
import com.stockline.product.dto.PageResponse;
import com.stockline.product.dto.SupplierRequest;
import com.stockline.product.dto.SupplierResponse;
import com.stockline.product.exception.DuplicateResourceException;
import com.stockline.product.exception.ResourceInUseException;
import com.stockline.product.exception.ResourceNotFoundException;
import com.stockline.product.service.SupplierService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
 * Web layer only; the service is a Mockito mock. Checks URLs, status codes, JSON shape and validation.
 */
@WebMvcTest(SupplierController.class)
@Import(TimeConfig.class)
class SupplierControllerTest {

	private static final Instant NOW = Instant.parse("2026-10-23T08:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SupplierService supplierService;

	@Test
	void list_withSearch_usesDefaultPageAndReturnsPageJson() throws Exception {
		// given
		SupplierResponse logiCo = new SupplierResponse(1L, "LogiCo", "Anna Pham", "anna@logico.com",
				"+84 90 111 2233", NOW, NOW);
		when(supplierService.list(eq("logi"), any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(logiCo), 0, 20, 1, 1));

		// when / then
		mockMvc.perform(get("/api/suppliers").param("search", "logi"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("LogiCo"))
				.andExpect(jsonPath("$.content[0].contactName").value("Anna Pham"))
				.andExpect(jsonPath("$.content[0].email").value("anna@logico.com"))
				.andExpect(jsonPath("$.content[0].phone").value("+84 90 111 2233"))
				.andExpect(jsonPath("$.totalElements").value(1));
		verify(supplierService).list(eq("logi"), argThat(pageable ->
				pageable.getPageNumber() == 0
						&& pageable.getPageSize() == 20
						&& pageable.getSort().equals(Sort.by("name"))));
	}

	@Test
	void get_existingId_returns200WithSupplier() throws Exception {
		// given
		when(supplierService.get(1L)).thenReturn(new SupplierResponse(1L, "LogiCo", "Anna Pham", "anna@logico.com",
				"+84 90 111 2233", NOW, NOW));

		// when / then
		mockMvc.perform(get("/api/suppliers/1"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("LogiCo"))
				.andExpect(jsonPath("$.email").value("anna@logico.com"));
	}

	@Test
	void update_validRequest_returns200WithUpdatedSupplier() throws Exception {
		// given
		when(supplierService.update(1L, new SupplierRequest("LogiCo", "Tom Lee", null, null)))
				.thenReturn(new SupplierResponse(1L, "LogiCo", "Tom Lee", null, null, NOW, NOW));

		// when / then
		mockMvc.perform(put("/api/suppliers/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "LogiCo", "contactName": "Tom Lee" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contactName").value("Tom Lee"))
				.andExpect(jsonPath("$.phone").isEmpty());
	}

	@Test
	void get_unknownId_returns404ProblemJson() throws Exception {
		// given
		when(supplierService.get(99L)).thenThrow(new ResourceNotFoundException("Supplier", 99L));

		// when / then
		mockMvc.perform(get("/api/suppliers/99"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Supplier with id 99 not found"))
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void create_validRequest_returns201WithLocation() throws Exception {
		// given
		when(supplierService.create(new SupplierRequest("WireCo", "Tom Lee", "tom@wireco.com", "+84 91 222 3344")))
				.thenReturn(new SupplierResponse(7L, "WireCo", "Tom Lee", "tom@wireco.com", "+84 91 222 3344", NOW, NOW));

		// when / then
		mockMvc.perform(post("/api/suppliers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "WireCo", "contactName": "Tom Lee",
								  "email": "tom@wireco.com", "phone": "+84 91 222 3344" }
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/suppliers/7"))
				.andExpect(jsonPath("$.id").value(7));
	}

	@Test
	void create_onlyName_returns201() throws Exception {
		// given
		when(supplierService.create(new SupplierRequest("DeskPro", null, null, null)))
				.thenReturn(new SupplierResponse(8L, "DeskPro", null, null, null, NOW, NOW));

		// when / then
		mockMvc.perform(post("/api/suppliers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "DeskPro" }
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").isEmpty());
	}

	@Test
	void create_invalidFields_returns400WithFieldErrors() throws Exception {
		// when / then
		mockMvc.perform(post("/api/suppliers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "", "email": "not-an-email", "phone": "call me" }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors.length()").value(3))
				.andExpect(jsonPath("$.errors[?(@.field == 'name')].message").value("must not be blank"))
				.andExpect(jsonPath("$.errors[?(@.field == 'email')].message")
						.value("must be a well-formed email address"))
				.andExpect(jsonPath("$.errors[?(@.field == 'phone')].message")
						.value("may contain only digits, spaces, +, -, ( and )"));
		verifyNoInteractions(supplierService);
	}

	@Test
	void update_duplicateName_returns409Duplicate() throws Exception {
		// given
		when(supplierService.update(1L, new SupplierRequest("WireCo", null, null, null)))
				.thenThrow(new DuplicateResourceException(MessageKeys.SUPPLIER_NAME_DUPLICATE, "WireCo"));

		// when / then
		mockMvc.perform(put("/api/suppliers/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "WireCo" }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE"));
	}

	@Test
	void delete_existingSupplier_returns204() throws Exception {
		// when / then
		mockMvc.perform(delete("/api/suppliers/1"))
				.andExpect(status().isNoContent());
		verify(supplierService).delete(1L);
	}

	@Test
	void delete_supplierInUse_returns409InUse() throws Exception {
		// given
		doThrow(new ResourceInUseException(MessageKeys.SUPPLIER_DELETE_HAS_PRODUCTS, "LogiCo"))
				.when(supplierService).delete(1L);

		// when / then
		mockMvc.perform(delete("/api/suppliers/1"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("IN_USE"));
	}
}
