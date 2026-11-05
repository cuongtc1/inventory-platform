package com.stockline.inventory.controller;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.DuplicateResourceException;
import com.stockline.common.exception.ResourceInUseException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.inventory.constant.MessageKeys;
import com.stockline.inventory.dto.WarehouseRequest;
import com.stockline.inventory.dto.WarehouseResponse;
import com.stockline.inventory.service.WarehouseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer only; the service is a Mockito mock. Checks URLs, status codes, JSON shape and validation.
 */
// Security filters off: this class checks URLs, JSON and errors; the role rules are tested in SecurityRulesTest
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(WarehouseController.class)
class WarehouseControllerTest {

	private static final Instant NOW = Instant.parse("2026-11-05T08:00:00Z");
	private static final WarehouseResponse MAIN = new WarehouseResponse(1L, "WH-01", "Main Warehouse",
			"District 7, HCMC", 412, NOW, "system", NOW, "manager@stockline.test");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WarehouseService warehouseService;

	@Test
	void list_withSearch_usesDefaultPageAndReturnsPageJson() throws Exception {
		// given
		when(warehouseService.list(eq("wh"), any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(MAIN), 0, 20, 1, 1));

		// when / then
		mockMvc.perform(get("/api/warehouses").param("search", "wh"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].code").value("WH-01"))
				.andExpect(jsonPath("$.content[0].name").value("Main Warehouse"))
				.andExpect(jsonPath("$.content[0].address").value("District 7, HCMC"))
				.andExpect(jsonPath("$.content[0].skuCount").value(412))
				.andExpect(jsonPath("$.content[0].updatedBy").value("manager@stockline.test"))
				.andExpect(jsonPath("$.totalElements").value(1));
		verify(warehouseService).list(eq("wh"), argThat(pageable ->
				pageable.getPageSize() == 20 && pageable.getSort().equals(Sort.by("code"))));
	}

	@Test
	void get_unknownId_returns404ProblemJson() throws Exception {
		// given
		when(warehouseService.get(9L)).thenThrow(new ResourceNotFoundException("Warehouse", 9L));

		// when / then
		mockMvc.perform(get("/api/warehouses/9"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("Warehouse with id 9 not found"));
	}

	@Test
	void create_validRequest_returns201WithLocation() throws Exception {
		// given
		when(warehouseService.create(new WarehouseRequest("WH-01", "Main Warehouse", "District 7, HCMC")))
				.thenReturn(MAIN);

		// when / then
		mockMvc.perform(post("/api/warehouses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "code": "WH-01", "name": "Main Warehouse", "address": "District 7, HCMC" }
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/warehouses/1"))
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void create_invalidFields_returns400WithFieldErrors() throws Exception {
		// when / then: a space isn't allowed in a code
		mockMvc.perform(post("/api/warehouses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "code": "WH 01", "name": " " }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("code", "name")));
		verifyNoInteractions(warehouseService);
	}

	@Test
	void update_duplicateCode_returns409Duplicate() throws Exception {
		// given
		when(warehouseService.update(eq(1L), any()))
				.thenThrow(new DuplicateResourceException(MessageKeys.WAREHOUSE_CODE_DUPLICATE, "WH-02"));

		// when / then
		mockMvc.perform(put("/api/warehouses/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "code": "WH-02", "name": "Main" }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE"))
				.andExpect(jsonPath("$.detail").value("Warehouse with code 'WH-02' already exists"));
	}

	@Test
	void delete_existingWarehouse_returns204() throws Exception {
		mockMvc.perform(delete("/api/warehouses/1")).andExpect(status().isNoContent());
		verify(warehouseService).delete(1L);
	}

	@Test
	void delete_withStock_returns409InUse() throws Exception {
		// given
		doThrow(new ResourceInUseException(MessageKeys.WAREHOUSE_DELETE_HAS_STOCK, "WH-01"))
				.when(warehouseService).delete(1L);

		// when / then
		mockMvc.perform(delete("/api/warehouses/1"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("IN_USE"))
				.andExpect(jsonPath("$.detail").value("Warehouse 'WH-01' still has stock levels, so it can not be deleted"));
	}
}
