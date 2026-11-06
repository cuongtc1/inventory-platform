package com.stockline.inventory.service;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.InvalidRequestException;
import com.stockline.inventory.constant.MessageKeys;
import com.stockline.inventory.constant.MovementType;
import com.stockline.inventory.dto.StockMovementFilter;
import com.stockline.inventory.dto.StockMovementResponse;
import com.stockline.inventory.entity.StockLevel;
import com.stockline.inventory.entity.StockMovement;
import com.stockline.inventory.mapper.StockMovementMapper;
import com.stockline.inventory.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

import static com.stockline.inventory.TestData.warehouse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

	private static final Pageable PAGEABLE = PageRequest.of(0, 20);

	@Mock
	private StockMovementRepository stockMovementRepository;

	private StockMovementService service;

	@BeforeEach
	void setUp() {
		service = new StockMovementService(stockMovementRepository, new StockMovementMapper());
	}

	@Test
	void list_mapsMovements() {
		// given
		StockLevel level = new StockLevel(warehouse(1L, "WH-01", "Main", null), 12L);
		level.copyProduct("A01", "Wireless Mouse", 20);
		when(stockMovementRepository.findAll(any(Specification.class), eq(PAGEABLE))).thenReturn(new PageImpl<>(
				List.of(new StockMovement(level, MovementType.ADJUST, -2, "Count")), PAGEABLE, 1));

		// when
		PageResponse<StockMovementResponse> page = service.list(filter(null, null), PAGEABLE);

		// then
		StockMovementResponse movement = page.content().getFirst();
		assertThat(movement.warehouseCode()).isEqualTo("WH-01");
		assertThat(movement.productName()).isEqualTo("Wireless Mouse");
		assertThat(movement.movementType()).isEqualTo(MovementType.ADJUST);
		assertThat(movement.quantity()).isEqualTo(-2);
		assertThat(movement.reference()).isEqualTo("Count");
	}

	@Test
	void list_fromAfterTo_isRejected() {
		// when / then
		assertThatThrownBy(() -> service.list(filter("2026-10-31", "2026-10-01"), PAGEABLE))
				.isInstanceOfSatisfying(InvalidRequestException.class,
						ex -> assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.STOCK_MOVEMENT_DATE_RANGE));
		verifyNoInteractions(stockMovementRepository);
	}

	@Test
	void list_sameDayTwice_isAValidRange() {
		// given
		when(stockMovementRepository.findAll(any(Specification.class), eq(PAGEABLE)))
				.thenReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

		// when
		PageResponse<StockMovementResponse> page = service.list(filter("2026-10-31", "2026-10-31"), PAGEABLE);

		// then
		assertThat(page.content()).isEmpty();
	}

	private static StockMovementFilter filter(String from, String to) {
		return new StockMovementFilter(null, null, null, null,
				from == null ? null : LocalDate.parse(from), to == null ? null : LocalDate.parse(to));
	}
}
