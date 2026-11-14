package com.stockline.inventory.service;

import com.stockline.inventory.constant.MovementType;
import com.stockline.inventory.entity.StockLevel;
import com.stockline.inventory.entity.StockMovement;
import com.stockline.inventory.entity.StockReservation;
import com.stockline.inventory.entity.Warehouse;
import com.stockline.inventory.event.OrderCreatedEvent;
import com.stockline.inventory.repository.StockLevelRepository;
import com.stockline.inventory.repository.StockMovementRepository;
import com.stockline.inventory.repository.StockReservationRepository;
import com.stockline.inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.Optional;

import static com.stockline.inventory.TestData.warehouse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * All or nothing, without a database: the repositories are mocks, the texts come from the real messages files.
 */
@ExtendWith(MockitoExtension.class)
class StockReserverTest {

	private static final Warehouse WH_01 = warehouse(2L, "WH-01", "Main", null);

	@Mock
	private StockLevelRepository stockLevelRepository;

	@Mock
	private StockMovementRepository stockMovementRepository;

	@Mock
	private StockReservationRepository stockReservationRepository;

	@Mock
	private WarehouseRepository warehouseRepository;

	private StockReserver reserver;

	@BeforeEach
	void setUp() {
		ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
		messages.setBasenames("messages", "common-messages");
		messages.setDefaultEncoding("UTF-8");
		messages.setFallbackToSystemLocale(false);
		reserver = new StockReserver(stockLevelRepository, stockMovementRepository, stockReservationRepository,
				warehouseRepository, messages, TransactionOperations.withoutTransaction());
	}

	@Test
	void everyLineFits_reservesAll_withAReservationAndAMovementEach() {
		// given
		StockLevel mouse = level(6L, "Wireless Mouse", 140, 0);
		StockLevel cable = level(7L, "USB-C Cable", 4, 0);
		givenWarehouse();
		when(stockLevelRepository.findByWarehouseIdAndProductId(2L, 6L)).thenReturn(Optional.of(mouse));
		when(stockLevelRepository.findByWarehouseIdAndProductId(2L, 7L)).thenReturn(Optional.of(cable));

		// when
		StockReserver.Result result = reserver.reserve(order(item(6L, "Wireless Mouse", 3), item(7L, "USB-C Cable", 4)));

		// then: on hand stays, reserved goes up, so available goes down
		assertThat(result).isEqualTo(new StockReserver.Reserved(1042));
		assertThat(mouse.getReserved()).isEqualTo(3);
		assertThat(mouse.getOnHand()).isEqualTo(140);
		assertThat(cable.getAvailable()).isZero();
		ArgumentCaptor<StockMovement> movements = ArgumentCaptor.forClass(StockMovement.class);
		verify(stockMovementRepository, org.mockito.Mockito.times(2)).save(movements.capture());
		assertThat(movements.getAllValues()).allSatisfy(movement -> {
			assertThat(movement.getMovementType()).isEqualTo(MovementType.RESERVE);
			assertThat(movement.getReference()).isEqualTo("SO-1042");
		});
		verify(stockReservationRepository, org.mockito.Mockito.times(2)).save(any(StockReservation.class));
		verify(stockLevelRepository).flush();
	}

	@Test
	void oneLineShort_reservesNothing_andSaysWhich() {
		// given: the first line fits, the second doesn't
		StockLevel mouse = level(6L, "Wireless Mouse", 140, 0);
		StockLevel cable = level(7L, "USB-C Cable", 4, 0);
		givenWarehouse();
		when(stockLevelRepository.findByWarehouseIdAndProductId(2L, 6L)).thenReturn(Optional.of(mouse));
		when(stockLevelRepository.findByWarehouseIdAndProductId(2L, 7L)).thenReturn(Optional.of(cable));

		// when
		StockReserver.Result result = reserver.reserve(order(item(6L, "Wireless Mouse", 3), item(7L, "USB-C Cable", 5)));

		// then: not even the first line was reserved
		assertThat(result).isEqualTo(new StockReserver.Failed(1042, "Not enough stock of 'USB-C Cable' in WH-01: only 4 available"));
		assertThat(mouse.getReserved()).isZero();
		verify(stockReservationRepository, never()).save(any());
		verify(stockMovementRepository, never()).save(any());
	}

	@Test
	void reservedStock_isNotAvailableToTheNextOrder() {
		// given: 10 on hand, 8 already promised to other orders
		givenWarehouse();
		when(stockLevelRepository.findByWarehouseIdAndProductId(2L, 6L))
				.thenReturn(Optional.of(level(6L, "Wireless Mouse", 10, 8)));

		// when / then
		assertThat(reserver.reserve(order(item(6L, "Wireless Mouse", 3))))
				.isEqualTo(new StockReserver.Failed(1042, "Not enough stock of 'Wireless Mouse' in WH-01: only 2 available"));
	}

	@Test
	void productNeverStockedHere_isShort_namedFromTheEvent() {
		// given
		givenWarehouse();
		when(stockLevelRepository.findByWarehouseIdAndProductId(2L, 99L)).thenReturn(Optional.empty());

		// when / then
		assertThat(reserver.reserve(order(item(99L, "Desk Lamp", 1))))
				.isEqualTo(new StockReserver.Failed(1042, "Not enough stock of 'Desk Lamp' in WH-01: only 0 available"));
	}

	@Test
	void unknownWarehouse_fails() {
		// given
		when(warehouseRepository.findById(2L)).thenReturn(Optional.empty());

		// when / then
		assertThat(reserver.reserve(order(item(6L, "Wireless Mouse", 1))))
				.isEqualTo(new StockReserver.Failed(1042, "Warehouse with id 2 not found"));
	}

	private void givenWarehouse() {
		when(warehouseRepository.findById(2L)).thenReturn(Optional.of(WH_01));
	}

	private static StockLevel level(long productId, String name, int onHand, int reserved) {
		StockLevel level = new StockLevel(WH_01, productId);
		level.copyProduct("SKU-" + productId, name, 0);
		level.setOnHand(onHand);
		level.setReserved(reserved);
		return level;
	}

	private static OrderCreatedEvent.Item item(long productId, String name, int quantity) {
		return new OrderCreatedEvent.Item(productId, name, quantity);
	}

	private static OrderCreatedEvent order(OrderCreatedEvent.Item... items) {
		return new OrderCreatedEvent(1042, "SO-1042", 2L, List.of(items));
	}
}
