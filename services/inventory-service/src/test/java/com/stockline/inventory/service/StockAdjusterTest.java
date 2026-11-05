package com.stockline.inventory.service;

import com.stockline.common.exception.InsufficientStockException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.inventory.constant.MessageKeys;
import com.stockline.inventory.constant.MovementType;
import com.stockline.inventory.dto.AdjustStockRequest;
import com.stockline.inventory.dto.ProductSnapshot;
import com.stockline.inventory.entity.StockLevel;
import com.stockline.inventory.entity.StockMovement;
import com.stockline.inventory.entity.Warehouse;
import com.stockline.inventory.repository.StockLevelRepository;
import com.stockline.inventory.repository.StockMovementRepository;
import com.stockline.inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.stockline.inventory.TestData.warehouse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test of the adjustment rules; repositories are Mockito mocks.
 */
@ExtendWith(MockitoExtension.class)
class StockAdjusterTest {

	private static final Long WAREHOUSE_ID = 1L;
	private static final Long PRODUCT_ID = 12L;
	private static final ProductSnapshot MOUSE = new ProductSnapshot(PRODUCT_ID, "A01", "Wireless Mouse", 20);

	@Mock
	private StockLevelRepository stockLevelRepository;

	@Mock
	private StockMovementRepository stockMovementRepository;

	@Mock
	private WarehouseRepository warehouseRepository;

	private final Warehouse main = warehouse(WAREHOUSE_ID, "WH-01", "Main", null);
	private StockAdjuster adjuster;

	@BeforeEach
	void setUp() {
		adjuster = new StockAdjuster(stockLevelRepository, stockMovementRepository, warehouseRepository);
		lenient().when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(main));
		lenient().when(stockLevelRepository.save(any(StockLevel.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void apply_firstAdjustment_createsStockLevelWithProductCopies() {
		// given
		when(stockLevelRepository.findByWarehouseIdAndProductId(WAREHOUSE_ID, PRODUCT_ID)).thenReturn(Optional.empty());

		// when
		StockLevel level = adjuster.apply(request(50, null), MOUSE);

		// then
		assertThat(level.getOnHand()).isEqualTo(50);
		assertThat(level.getAvailable()).isEqualTo(50);
		assertThat(level.getProductSku()).isEqualTo("A01");
		assertThat(level.getProductName()).isEqualTo("Wireless Mouse");
		assertThat(level.getReorderLevel()).isEqualTo(20);
		assertThat(level.getWarehouse()).isSameAs(main);
	}

	@Test
	void apply_writesAnAdjustMovementAndFlushes() {
		// given
		when(stockLevelRepository.findByWarehouseIdAndProductId(WAREHOUSE_ID, PRODUCT_ID))
				.thenReturn(Optional.of(level(100, 0)));

		// when
		adjuster.apply(request(-2, " Count "), MOUSE);

		// then
		ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
		verify(stockMovementRepository).save(movement.capture());
		assertThat(movement.getValue().getMovementType()).isEqualTo(MovementType.ADJUST);
		assertThat(movement.getValue().getQuantity()).isEqualTo(-2);
		assertThat(movement.getValue().getReference()).isEqualTo("Count");
		assertThat(movement.getValue().getProductSku()).isEqualTo("A01");
		assertThat(movement.getValue().getProductName()).isEqualTo("Wireless Mouse");
		verify(stockLevelRepository).flush();
	}

	@Test
	void apply_refreshesCopiesOfAnExistingRow() {
		// given: the product was renamed and its reorder level changed since the last adjustment
		StockLevel existing = level(10, 0);
		existing.copyProduct("OLD", "Old name", 1);
		when(stockLevelRepository.findByWarehouseIdAndProductId(WAREHOUSE_ID, PRODUCT_ID))
				.thenReturn(Optional.of(existing));

		// when
		adjuster.apply(request(1, null), MOUSE);

		// then
		assertThat(existing.getProductName()).isEqualTo("Wireless Mouse");
		assertThat(existing.getReorderLevel()).isEqualTo(20);
	}

	@Test
	void apply_canTakeEverythingAvailable_butNotReservedStock() {
		// given: 10 on hand, 4 reserved, so 6 available
		StockLevel existing = level(10, 4);
		when(stockLevelRepository.findByWarehouseIdAndProductId(WAREHOUSE_ID, PRODUCT_ID))
				.thenReturn(Optional.of(existing));

		// when
		adjuster.apply(request(-6, null), MOUSE);

		// then
		assertThat(existing.getOnHand()).isEqualTo(4);
		assertThat(existing.getAvailable()).isZero();
	}

	@Test
	void apply_moreThanAvailable_throwsAndChangesNothing() {
		// given
		StockLevel existing = level(10, 4);
		when(stockLevelRepository.findByWarehouseIdAndProductId(WAREHOUSE_ID, PRODUCT_ID))
				.thenReturn(Optional.of(existing));

		// when / then
		assertThatThrownBy(() -> adjuster.apply(request(-7, null), MOUSE))
				.isInstanceOfSatisfying(InsufficientStockException.class, ex -> {
					assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.STOCK_INSUFFICIENT);
					assertThat(ex.getMessageArgs()).containsExactly("Wireless Mouse", "WH-01", 6);
				});
		assertThat(existing.getOnHand()).isEqualTo(10);
		verify(stockMovementRepository, never()).save(any());
	}

	@Test
	void apply_firstAdjustmentNegative_throws() {
		// given
		when(stockLevelRepository.findByWarehouseIdAndProductId(WAREHOUSE_ID, PRODUCT_ID)).thenReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> adjuster.apply(request(-1, null), MOUSE))
				.isInstanceOf(InsufficientStockException.class);
		verify(stockLevelRepository, never()).save(any());
	}

	@Test
	void apply_unknownWarehouse_throwsNotFound() {
		// given
		when(warehouseRepository.findById(9L)).thenReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> adjuster.apply(new AdjustStockRequest(PRODUCT_ID, 9L, 5, null), MOUSE))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private AdjustStockRequest request(int quantity, String reference) {
		return new AdjustStockRequest(PRODUCT_ID, WAREHOUSE_ID, quantity, reference);
	}

	private StockLevel level(int onHand, int reserved) {
		StockLevel level = new StockLevel(main, PRODUCT_ID);
		level.setOnHand(onHand);
		level.setReserved(reserved);
		return level;
	}
}
