package com.stockline.inventory.service;

import com.stockline.common.exception.InvalidRequestException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.inventory.client.ProductClient;
import com.stockline.inventory.constant.MessageKeys;
import com.stockline.inventory.dto.AdjustStockRequest;
import com.stockline.inventory.dto.ProductSnapshot;
import com.stockline.inventory.dto.StockLevelResponse;
import com.stockline.inventory.entity.StockLevel;
import com.stockline.inventory.mapper.StockLevelMapper;
import com.stockline.inventory.repository.StockLevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.stockline.inventory.TestData.warehouse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockLevelServiceTest {

	@Mock
	private StockLevelRepository stockLevelRepository;

	@Mock
	private ProductClient productClient;

	@Mock
	private StockAdjuster stockAdjuster;

	private StockLevelService service;

	@BeforeEach
	void setUp() {
		service = new StockLevelService(stockLevelRepository, new StockLevelMapper(), productClient, stockAdjuster);
	}

	@Test
	void get_calculatesAvailableAndLowStock() {
		// given: 20 available, reorder level 20: at the level counts as low
		when(stockLevelRepository.findById(1L)).thenReturn(Optional.of(level(25, 5, 20)));

		// when
		StockLevelResponse response = service.get(1L);

		// then
		assertThat(response.available()).isEqualTo(20);
		assertThat(response.lowStock()).isTrue();
		assertThat(response.warehouseCode()).isEqualTo("WH-01");
	}

	@Test
	void get_aboveReorderLevel_isNotLow() {
		// given
		when(stockLevelRepository.findById(1L)).thenReturn(Optional.of(level(25, 4, 20)));

		// when / then
		assertThat(service.get(1L).lowStock()).isFalse();
	}

	@Test
	void get_unknownId_throwsNotFound() {
		// given
		when(stockLevelRepository.findById(9L)).thenReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> service.get(9L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void adjust_readsTheProductAndHandsItToTheAdjuster() {
		// given
		AdjustStockRequest request = new AdjustStockRequest(12L, 1L, 5, null);
		ProductSnapshot mouse = new ProductSnapshot(12L, "A01", "Wireless Mouse", 20);
		when(productClient.getProduct(12L)).thenReturn(mouse);
		when(stockAdjuster.apply(request, mouse)).thenReturn(level(5, 0, 20));

		// when
		StockLevelResponse response = service.adjust(request);

		// then
		assertThat(response.onHand()).isEqualTo(5);
	}

	@Test
	void adjust_zero_isRejectedBeforeCallingAnything() {
		// when / then
		assertThatThrownBy(() -> service.adjust(new AdjustStockRequest(12L, 1L, 0, null)))
				.isInstanceOfSatisfying(InvalidRequestException.class,
						ex -> assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.STOCK_ADJUST_ZERO));
		verifyNoInteractions(productClient, stockAdjuster);
	}

	@Test
	void adjust_unknownProduct_changesNothing() {
		// given
		when(productClient.getProduct(12L)).thenThrow(new ResourceNotFoundException("Product", 12L));

		// when / then
		assertThatThrownBy(() -> service.adjust(new AdjustStockRequest(12L, 1L, 5, null)))
				.isInstanceOf(ResourceNotFoundException.class);
		verifyNoInteractions(stockAdjuster);
	}

	private static StockLevel level(int onHand, int reserved, int reorderLevel) {
		StockLevel level = new StockLevel(warehouse(1L, "WH-01", "Main", null), 12L);
		level.copyProduct("A01", "Wireless Mouse", reorderLevel);
		level.setOnHand(onHand);
		level.setReserved(reserved);
		return level;
	}
}
