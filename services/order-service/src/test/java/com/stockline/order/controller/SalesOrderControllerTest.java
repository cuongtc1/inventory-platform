package com.stockline.order.controller;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.InvalidRequestException;
import com.stockline.common.exception.InvalidStatusTransitionException;
import com.stockline.order.constant.MessageKeys;
import com.stockline.order.constant.OrderStatus;
import com.stockline.order.dto.CreateSalesOrderRequest;
import com.stockline.order.dto.OrderStatusHistoryResponse;
import com.stockline.order.dto.SalesOrderFilter;
import com.stockline.order.dto.SalesOrderResponse;
import com.stockline.order.dto.SalesOrderSummaryResponse;
import com.stockline.order.service.OrderStatusService;
import com.stockline.order.service.SalesOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer only; the service is a Mockito mock. Checks URLs, JSON, the filters and the validation of the lines.
 */
// Security filters off: this class checks URLs, JSON and errors; the role rules are tested in SecurityRulesTest
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(SalesOrderController.class)
class SalesOrderControllerTest {

	private static final Instant NOW = Instant.parse("2026-11-10T03:15:00Z");
	private static final SalesOrderResponse ORDER = new SalesOrderResponse(5L, "SO-1042", 1L, "ACME Ltd", 2L, "WH-01",
			OrderStatus.PENDING, new BigDecimal("108.00"),
			List.of(new SalesOrderResponse.Item(9L, 6L, "A01", "Wireless Mouse", 5, new BigDecimal("12.00"),
					new BigDecimal("60.00"))),
			NOW, "staff@stockline.test", NOW, "staff@stockline.test");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SalesOrderService salesOrderService;

	@MockitoBean
	private OrderStatusService orderStatusService;

	@Test
	void actions_askForTheirStatus() throws Exception {
		// given
		when(orderStatusService.changeStatus(eq(5L), any(), eq(null))).thenReturn(ORDER);

		// when
		for (String verb : List.of("pay", "ship", "complete", "cancel")) {
			mockMvc.perform(post("/api/sales-orders/5/{verb}", verb)).andExpect(status().isOk());
		}

		// then
		verify(orderStatusService).changeStatus(5L, OrderStatus.PAID, null);
		verify(orderStatusService).changeStatus(5L, OrderStatus.SHIPPED, null);
		verify(orderStatusService).changeStatus(5L, OrderStatus.COMPLETED, null);
		verify(orderStatusService).changeStatus(5L, OrderStatus.CANCELLED, null);
	}

	@Test
	void action_notAllowed_returns409WithTheText() throws Exception {
		// given
		when(orderStatusService.changeStatus(5L, OrderStatus.SHIPPED, null)).thenThrow(new InvalidStatusTransitionException(
				MessageKeys.ORDER_STATUS_CHANGE_NOT_ALLOWED, "SO-1042", OrderStatus.PENDING, OrderStatus.SHIPPED));

		// when / then
		mockMvc.perform(post("/api/sales-orders/5/ship"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"))
				.andExpect(jsonPath("$.detail").value("Order SO-1042 is PENDING, so it can not change to SHIPPED"));
	}

	@Test
	void history_returnsTheTimeline() throws Exception {
		// given
		when(orderStatusService.history(5L)).thenReturn(List.of(
				new OrderStatusHistoryResponse(1L, null, OrderStatus.PENDING, null, NOW, "staff@stockline.test"),
				new OrderStatusHistoryResponse(2L, OrderStatus.PENDING, OrderStatus.CANCELLED, null, NOW, "manager@stockline.test")));

		// when / then
		mockMvc.perform(get("/api/sales-orders/5/history"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].fromStatus").doesNotExist())
				.andExpect(jsonPath("$[0].toStatus").value("PENDING"))
				.andExpect(jsonPath("$[1].createdBy").value("manager@stockline.test"));
	}

	@Test
	void create_validRequest_returns201WithLocationAndLines() throws Exception {
		// given
		when(salesOrderService.create(new CreateSalesOrderRequest(1L, 2L,
				List.of(new CreateSalesOrderRequest.Item(6L, 5))))).thenReturn(ORDER);

		// when / then: money as plain numbers with 2 decimals
		mockMvc.perform(post("/api/sales-orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "customerId": 1, "warehouseId": 2, "items": [ { "productId": 6, "quantity": 5 } ] }
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/sales-orders/5"))
				.andExpect(jsonPath("$.orderNumber").value("SO-1042"))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.totalAmount").value(108.00))
				.andExpect(jsonPath("$.items[0].productName").value("Wireless Mouse"))
				.andExpect(jsonPath("$.items[0].lineTotal").value(60.00))
				.andExpect(jsonPath("$.createdBy").value("staff@stockline.test"));
	}

	@Test
	void create_noLines_returns400OnItems() throws Exception {
		mockMvc.perform(post("/api/sales-orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "customerId": 1, "warehouseId": 2, "items": [] }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors[0].field").value("items"));
		verifyNoInteractions(salesOrderService);
	}

	@Test
	void create_badLine_namesTheLineInTheError() throws Exception {
		mockMvc.perform(post("/api/sales-orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "customerId": 1,
								  "items": [ { "productId": 6, "quantity": 5 }, { "productId": null, "quantity": 0 } ] }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field",
						containsInAnyOrder("warehouseId", "items[1].productId", "items[1].quantity")));
		verifyNoInteractions(salesOrderService);
	}

	@Test
	void create_inactiveProduct_returns400WithTheText() throws Exception {
		// given
		when(salesOrderService.create(any()))
				.thenThrow(new InvalidRequestException(MessageKeys.ORDER_PRODUCT_INACTIVE, "A01"));

		// when / then
		mockMvc.perform(post("/api/sales-orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "customerId": 1, "warehouseId": 2, "items": [ { "productId": 6, "quantity": 5 } ] }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.detail").value("Product 'A01' is inactive and can not be sold"));
	}

	@Test
	void list_bindsFilters_newestFirstByDefault() throws Exception {
		// given
		SalesOrderSummaryResponse row = new SalesOrderSummaryResponse(5L, "SO-1042", 1L, "ACME Ltd", "WH-01",
				OrderStatus.PENDING, new BigDecimal("108.00"), NOW, "staff@stockline.test");
		when(salesOrderService.list(any(), any())).thenReturn(new PageResponse<>(List.of(row), 0, 20, 1, 1));

		// when
		mockMvc.perform(get("/api/sales-orders").param("search", "acme").param("customerId", "1")
						.param("status", "PENDING"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].customerName").value("ACME Ltd"))
				.andExpect(jsonPath("$.content[0].items").doesNotExist());

		// then
		verify(salesOrderService).list(eq(new SalesOrderFilter("acme", 1L, OrderStatus.PENDING)),
				argThat((Pageable pageable) -> pageable.getPageSize() == 20
						&& pageable.getSort().equals(Sort.by(Sort.Direction.DESC, "createdAt"))));
	}

	@Test
	void list_unknownStatus_returns400() throws Exception {
		mockMvc.perform(get("/api/sales-orders").param("status", "LOST"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("status"));
		verifyNoInteractions(salesOrderService);
	}

	@Test
	void get_returnsTheOrder() throws Exception {
		// given
		when(salesOrderService.get(5L)).thenReturn(ORDER);

		// when / then
		mockMvc.perform(get("/api/sales-orders/5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].unitPrice").value(12.00));
	}
}
