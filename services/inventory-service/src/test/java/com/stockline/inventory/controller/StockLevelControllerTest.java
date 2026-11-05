package com.stockline.inventory.controller;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.InsufficientStockException;
import com.stockline.inventory.constant.MessageKeys;
import com.stockline.inventory.dto.AdjustStockRequest;
import com.stockline.inventory.dto.StockLevelFilter;
import com.stockline.inventory.dto.StockLevelResponse;
import com.stockline.inventory.service.StockLevelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer only; the service is a Mockito mock. Checks URLs, JSON, validation and error format.
 */
// Security filters off: this class checks URLs, JSON and errors; the role rules are tested in SecurityRulesTest
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(StockLevelController.class)
class StockLevelControllerTest {

	private static final Instant NOW = Instant.parse("2026-11-05T08:00:00Z");
	private static final StockLevelResponse MOUSE = new StockLevelResponse(3L, 1L, "WH-01", "Main", 12L, "A01",
			"Wireless Mouse", 140, 20, 120, 20, false, NOW, "system", NOW, "manager@stockline.test");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StockLevelService stockLevelService;

	@Test
	void list_bindsFiltersAndReturnsCalculatedFields() throws Exception {
		// given
		when(stockLevelService.list(eq(new StockLevelFilter("mouse", 1L, null, true)), any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(MOUSE), 0, 20, 1, 1));

		// when / then
		mockMvc.perform(get("/api/stock-levels")
						.param("search", "mouse").param("warehouseId", "1").param("lowStock", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].productName").value("Wireless Mouse"))
				.andExpect(jsonPath("$.content[0].warehouseCode").value("WH-01"))
				.andExpect(jsonPath("$.content[0].onHand").value(140))
				.andExpect(jsonPath("$.content[0].reserved").value(20))
				.andExpect(jsonPath("$.content[0].available").value(120))
				.andExpect(jsonPath("$.content[0].lowStock").value(false));
	}

	@Test
	void adjust_valid_returns200() throws Exception {
		// given
		when(stockLevelService.adjust(new AdjustStockRequest(12L, 1L, -2, "Count"))).thenReturn(MOUSE);

		// when / then
		mockMvc.perform(post("/api/stock-levels/adjust")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "productId": 12, "warehouseId": 1, "quantity": -2, "reference": "Count" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(3));
	}

	@Test
	void adjust_missingFields_returns400() throws Exception {
		// when / then
		mockMvc.perform(post("/api/stock-levels/adjust")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("productId", "warehouseId", "quantity")));
		verifyNoInteractions(stockLevelService);
	}

	@Test
	void adjust_tooLarge_returns400() throws Exception {
		// when / then
		mockMvc.perform(post("/api/stock-levels/adjust")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "productId": 12, "warehouseId": 1, "quantity": 2000000 }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("quantity"));
	}

	@Test
	void adjust_notEnoughStock_returns409() throws Exception {
		// given
		when(stockLevelService.adjust(any())).thenThrow(
				new InsufficientStockException(MessageKeys.STOCK_INSUFFICIENT, "Wireless Mouse", "WH-01", 1234));

		// when / then
		mockMvc.perform(post("/api/stock-levels/adjust")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "productId": 12, "warehouseId": 1, "quantity": -5000 }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
				.andExpect(jsonPath("$.detail").value("Not enough stock of 'Wireless Mouse' in WH-01: only 1234 available"));
	}

	@Test
	void adjust_someoneElseChangedTheRow_returns409ConcurrentUpdate() throws Exception {
		// given: what Spring throws when @Version finds the row changed
		when(stockLevelService.adjust(any())).thenThrow(
				new ObjectOptimisticLockingFailureException("StockLevel", 3L));

		// when / then
		mockMvc.perform(post("/api/stock-levels/adjust")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "productId": 12, "warehouseId": 1, "quantity": 5 }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"));
	}
}
