package com.stockline.order.service;

import com.stockline.common.exception.InvalidRequestException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.order.client.ProductClient;
import com.stockline.order.client.WarehouseClient;
import com.stockline.order.constant.MessageKeys;
import com.stockline.order.dto.CreateSalesOrderRequest;
import com.stockline.order.dto.CreateSalesOrderRequest.Item;
import com.stockline.order.dto.ProductSnapshot;
import com.stockline.order.dto.SalesOrderResponse;
import com.stockline.order.dto.WarehouseSnapshot;
import com.stockline.order.entity.SalesOrder;
import com.stockline.order.mapper.SalesOrderMapper;
import com.stockline.order.repository.CustomerRepository;
import com.stockline.order.repository.SalesOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.stockline.order.TestData.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The checks before an order is saved. The clients and the creator are Mockito mocks: this test only decides what
 * reaches them.
 */
@ExtendWith(MockitoExtension.class)
class SalesOrderServiceTest {

	private static final WarehouseSnapshot WH_01 = new WarehouseSnapshot(2L, "WH-01");
	private static final ProductSnapshot MOUSE = new ProductSnapshot(6L, "A01", "Wireless Mouse", new BigDecimal("12.00"), "ACTIVE");
	private static final ProductSnapshot STAND = new ProductSnapshot(7L, "A02", "Laptop Stand", new BigDecimal("24.00"), "ACTIVE");

	@Mock
	private SalesOrderRepository salesOrderRepository;

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private ProductClient productClient;

	@Mock
	private WarehouseClient warehouseClient;

	@Mock
	private SalesOrderCreator salesOrderCreator;

	private SalesOrderService service;

	@BeforeEach
	void setUp() {
		service = new SalesOrderService(salesOrderRepository, customerRepository, new SalesOrderMapper(), productClient,
				warehouseClient, salesOrderCreator);
	}

	@Test
	void create_readsWarehouseAndProducts_andHandsThemToTheCreator() {
		// given
		CreateSalesOrderRequest request = request(new Item(6L, 5), new Item(7L, 2));
		when(customerRepository.existsById(1L)).thenReturn(true);
		when(warehouseClient.getWarehouse(2L)).thenReturn(WH_01);
		when(productClient.getProduct(6L)).thenReturn(MOUSE);
		when(productClient.getProduct(7L)).thenReturn(STAND);
		SalesOrder saved = new SalesOrder("SO-1001", customer(1L, "ACME Ltd", null, null), 2L, "WH-01");
		saved.addItem(6L, "A01", "Wireless Mouse", 5, new BigDecimal("12.00"));
		saved.addItem(7L, "A02", "Laptop Stand", 2, new BigDecimal("24.00"));
		when(salesOrderCreator.create(eq(request), eq(WH_01), anyMap())).thenReturn(saved);

		// when
		SalesOrderResponse response = service.create(request);

		// then: the mockup's example, 5 × $12.00 + 2 × $24.00
		assertThat(response.totalAmount()).isEqualByComparingTo("108.00");
		assertThat(response.items()).extracting(SalesOrderResponse.Item::lineTotal)
				.containsExactly(new BigDecimal("60.00"), new BigDecimal("48.00"));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<Long, ProductSnapshot>> products = ArgumentCaptor.forClass(Map.class);
		verify(salesOrderCreator).create(eq(request), eq(WH_01), products.capture());
		assertThat(products.getValue()).containsOnlyKeys(6L, 7L);
	}

	@Test
	void create_sameProductTwice_isRefusedBeforeAnyCall() {
		// when / then
		assertThatThrownBy(() -> service.create(request(new Item(6L, 5), new Item(6L, 1))))
				.isInstanceOfSatisfying(InvalidRequestException.class, ex -> {
					assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.ORDER_PRODUCT_TWICE);
					assertThat(ex.getMessageArgs()).containsExactly(6L);
				});
		verifyNoInteractions(customerRepository, warehouseClient, productClient, salesOrderCreator);
	}

	@Test
	void create_unknownCustomer_isRefusedBeforeCallingOtherServices() {
		// given
		when(customerRepository.existsById(1L)).thenReturn(false);

		// when / then
		assertThatThrownBy(() -> service.create(request(new Item(6L, 5))))
				.isInstanceOf(ResourceNotFoundException.class);
		verifyNoInteractions(warehouseClient, productClient, salesOrderCreator);
	}

	@Test
	void create_inactiveProduct_isRefused() {
		// given
		when(customerRepository.existsById(1L)).thenReturn(true);
		when(warehouseClient.getWarehouse(2L)).thenReturn(WH_01);
		when(productClient.getProduct(6L)).thenReturn(
				new ProductSnapshot(6L, "A01", "Wireless Mouse", new BigDecimal("12.00"), "INACTIVE"));

		// when / then
		assertThatThrownBy(() -> service.create(request(new Item(6L, 5))))
				.isInstanceOfSatisfying(InvalidRequestException.class, ex -> {
					assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.ORDER_PRODUCT_INACTIVE);
					assertThat(ex.getMessageArgs()).containsExactly("A01");
				});
		verifyNoInteractions(salesOrderCreator);
	}

	@Test
	void create_totalAboveDecimal12_2_isRefusedBeforeSaving() {
		// given: 1,000,000 × $10,000.00 = $10,000,000,000.00, one cent above the column's limit
		when(customerRepository.existsById(1L)).thenReturn(true);
		when(warehouseClient.getWarehouse(2L)).thenReturn(WH_01);
		when(productClient.getProduct(6L)).thenReturn(
				new ProductSnapshot(6L, "A01", "Server", new BigDecimal("10000.00"), "ACTIVE"));

		// when / then
		assertThatThrownBy(() -> service.create(request(new Item(6L, 1_000_000))))
				.isInstanceOfSatisfying(InvalidRequestException.class, ex -> {
					assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.ORDER_TOTAL_TOO_LARGE);
					assertThat(ex.getMessageArgs()).containsExactly("$10,000,000,000.00", "$9,999,999,999.99");
				});
		verifyNoInteractions(salesOrderCreator);
	}

	@Test
	void create_unknownProduct_stopsWithItsNotFound() {
		// given
		when(customerRepository.existsById(1L)).thenReturn(true);
		when(warehouseClient.getWarehouse(2L)).thenReturn(WH_01);
		when(productClient.getProduct(99L)).thenThrow(new ResourceNotFoundException("Product", 99L));

		// when / then
		assertThatThrownBy(() -> service.create(request(new Item(99L, 1))))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(salesOrderCreator, org.mockito.Mockito.never()).create(any(), any(), anyMap());
	}

	private static CreateSalesOrderRequest request(Item... items) {
		return new CreateSalesOrderRequest(1L, 2L, List.of(items));
	}
}
