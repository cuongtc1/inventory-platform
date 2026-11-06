package com.stockline.inventory.controller;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.InvalidRequestException;
import com.stockline.inventory.constant.MessageKeys;
import com.stockline.inventory.constant.MovementType;
import com.stockline.inventory.dto.StockMovementFilter;
import com.stockline.inventory.dto.StockMovementResponse;
import com.stockline.inventory.service.StockMovementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer only; the service is a Mockito mock. Most important here: the filters reach the service with the right
 * types, and bad values are a 400.
 */
// Security filters off: this class checks URLs, JSON and errors; the role rules are tested in SecurityRulesTest
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(StockMovementController.class)
class StockMovementControllerTest {

	private static final Instant NOW = Instant.parse("2026-11-05T08:00:00Z");
	private static final StockMovementResponse MOVEMENT = new StockMovementResponse(5L, 1L, "WH-01", 12L, "A01",
			"Wireless Mouse", MovementType.ADJUST, -2, "Count", NOW, "manager@stockline.test", NOW,
			"manager@stockline.test");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StockMovementService stockMovementService;

	@Test
	void list_bindsEveryFilterAndReturnsJson() throws Exception {
		// given
		StockMovementFilter filter = new StockMovementFilter("mouse", 1L, 12L, MovementType.ADJUST,
				LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));
		when(stockMovementService.list(eq(filter), any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(MOVEMENT), 0, 20, 1, 1));

		// when / then
		mockMvc.perform(get("/api/stock-movements")
						.param("search", "mouse").param("warehouseId", "1").param("productId", "12")
						.param("movementType", "ADJUST").param("from", "2026-10-01").param("to", "2026-10-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].warehouseCode").value("WH-01"))
				.andExpect(jsonPath("$.content[0].productName").value("Wireless Mouse"))
				.andExpect(jsonPath("$.content[0].movementType").value("ADJUST"))
				.andExpect(jsonPath("$.content[0].quantity").value(-2))
				.andExpect(jsonPath("$.content[0].reference").value("Count"))
				.andExpect(jsonPath("$.content[0].createdAt").value("2026-11-05T08:00:00Z"))
				.andExpect(jsonPath("$.content[0].createdBy").value("manager@stockline.test"));
	}

	@Test
	void list_noParameters_newestFirst() throws Exception {
		// given
		when(stockMovementService.list(any(), any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		// when
		mockMvc.perform(get("/api/stock-movements")).andExpect(status().isOk());

		// then
		verify(stockMovementService).list(eq(new StockMovementFilter(null, null, null, null, null, null)),
				argThat(pageable -> pageable.getPageSize() == 20
						&& pageable.getSort().equals(Sort.by(Sort.Direction.DESC, "createdAt"))));
	}

	@Test
	void list_wrongDateFormat_returns400() throws Exception {
		// when / then
		mockMvc.perform(get("/api/stock-movements").param("from", "31/10/2026"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors[0].field").value("from"));
		verifyNoInteractions(stockMovementService);
	}

	@Test
	void list_unknownType_returns400() throws Exception {
		// when / then
		mockMvc.perform(get("/api/stock-movements").param("movementType", "STOLEN"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("movementType"));
		verifyNoInteractions(stockMovementService);
	}

	@Test
	void list_fromAfterTo_returns400WithTheText() throws Exception {
		// given
		when(stockMovementService.list(any(), any()))
				.thenThrow(new InvalidRequestException(MessageKeys.STOCK_MOVEMENT_DATE_RANGE));

		// when / then
		mockMvc.perform(get("/api/stock-movements").param("from", "2026-10-31").param("to", "2026-10-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.detail").value("The from date must not be after the to date"));
	}

	@Test
	void post_isNotAllowed() throws Exception {
		// Movements are written by stock changes only
		mockMvc.perform(post("/api/stock-movements"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}
}
