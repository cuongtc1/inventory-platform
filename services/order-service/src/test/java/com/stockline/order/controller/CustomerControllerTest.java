package com.stockline.order.controller;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.DuplicateResourceException;
import com.stockline.common.exception.ResourceInUseException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.order.constant.MessageKeys;
import com.stockline.order.dto.CustomerRequest;
import com.stockline.order.dto.CustomerResponse;
import com.stockline.order.service.CustomerService;
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
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

	private static final Instant NOW = Instant.parse("2026-11-07T08:00:00Z");
	private static final CustomerResponse ACME = new CustomerResponse(1L, "ACME Ltd", "buy@acme.com",
			"+84 28 3800 1122", 18, NOW, "system", NOW, "staff@stockline.test");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CustomerService customerService;

	@Test
	void list_withSearch_usesDefaultPageAndReturnsPageJson() throws Exception {
		// given
		when(customerService.list(eq("acme"), any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(ACME), 0, 20, 1, 1));

		// when / then
		mockMvc.perform(get("/api/customers").param("search", "acme"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("ACME Ltd"))
				.andExpect(jsonPath("$.content[0].email").value("buy@acme.com"))
				.andExpect(jsonPath("$.content[0].phone").value("+84 28 3800 1122"))
				.andExpect(jsonPath("$.content[0].orderCount").value(18))
				.andExpect(jsonPath("$.content[0].updatedBy").value("staff@stockline.test"))
				.andExpect(jsonPath("$.totalElements").value(1));
		verify(customerService).list(eq("acme"), argThat(pageable ->
				pageable.getPageSize() == 20 && pageable.getSort().equals(Sort.by("name"))));
	}

	@Test
	void get_unknownId_returns404ProblemJson() throws Exception {
		// given
		when(customerService.get(9L)).thenThrow(new ResourceNotFoundException("Customer", 9L));

		// when / then
		mockMvc.perform(get("/api/customers/9"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("Customer with id 9 not found"));
	}

	@Test
	void create_validRequest_returns201WithLocation() throws Exception {
		// given
		when(customerService.create(new CustomerRequest("ACME Ltd", "buy@acme.com", "+84 28 3800 1122")))
				.thenReturn(ACME);

		// when / then
		mockMvc.perform(post("/api/customers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "ACME Ltd", "email": "buy@acme.com", "phone": "+84 28 3800 1122" }
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/customers/1"))
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void create_invalidFields_returns400WithFieldErrors() throws Exception {
		// when / then
		mockMvc.perform(post("/api/customers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": " ", "email": "not an email", "phone": "call me" }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "email", "phone")));
		verifyNoInteractions(customerService);
	}

	@Test
	void update_duplicateName_returns409Duplicate() throws Exception {
		// given
		when(customerService.update(eq(1L), any()))
				.thenThrow(new DuplicateResourceException(MessageKeys.CUSTOMER_NAME_DUPLICATE, "Nova Retail"));

		// when / then
		mockMvc.perform(put("/api/customers/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Nova Retail" }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE"))
				.andExpect(jsonPath("$.detail").value("Customer 'Nova Retail' already exists"));
	}

	@Test
	void delete_existingCustomer_returns204() throws Exception {
		mockMvc.perform(delete("/api/customers/1")).andExpect(status().isNoContent());
		verify(customerService).delete(1L);
	}

	@Test
	void delete_withOrders_returns409InUse() throws Exception {
		// given
		doThrow(new ResourceInUseException(MessageKeys.CUSTOMER_DELETE_HAS_ORDERS, "ACME Ltd"))
				.when(customerService).delete(1L);

		// when / then
		mockMvc.perform(delete("/api/customers/1"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("IN_USE"))
				.andExpect(jsonPath("$.detail").value("Customer 'ACME Ltd' has sales orders, so it can not be deleted"));
	}
}
