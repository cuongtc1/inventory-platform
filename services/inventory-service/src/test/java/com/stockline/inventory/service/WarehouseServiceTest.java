package com.stockline.inventory.service;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.DuplicateResourceException;
import com.stockline.common.exception.ResourceInUseException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.inventory.constant.MessageKeys;
import com.stockline.inventory.dto.WarehouseRequest;
import com.stockline.inventory.dto.WarehouseResponse;
import com.stockline.inventory.entity.Warehouse;
import com.stockline.inventory.mapper.WarehouseMapper;
import com.stockline.inventory.repository.WarehouseRepository;
import com.stockline.inventory.repository.WarehouseRepository.WarehouseSkuCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.stockline.inventory.TestData.warehouse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test: no Spring, no database. The repository is a Mockito mock; the mapper is the real class.
 */
@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

	@Mock
	private WarehouseRepository warehouseRepository;

	private WarehouseService warehouseService;

	@BeforeEach
	void setUp() {
		warehouseService = new WarehouseService(warehouseRepository, new WarehouseMapper());
	}

	@Test
	void list_searchWithSpaces_trimsItAndAddsSkuCounts() {
		// given: WH-02 has no stock levels, so it isn't in the count result
		Pageable pageable = PageRequest.of(0, 20);
		Warehouse main = warehouse(1L, "WH-01", "Main Warehouse", "District 7");
		Warehouse north = warehouse(2L, "WH-02", "North Hub", null);
		when(warehouseRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase("wh", "wh", pageable))
				.thenReturn(new PageImpl<>(List.of(main, north), pageable, 2));
		when(warehouseRepository.countSkus(List.of(1L, 2L))).thenReturn(List.of(skuCount(1L, 412)));

		// when
		PageResponse<WarehouseResponse> page = warehouseService.list(" wh  ", pageable);

		// then
		assertThat(page.totalElements()).isEqualTo(2);
		assertThat(page.content()).extracting(WarehouseResponse::skuCount).containsExactly(412L, 0L);
	}

	@Test
	void list_emptyPage_doesNotCount() {
		// given
		Pageable pageable = PageRequest.of(0, 20);
		when(warehouseRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase("", "", pageable))
				.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		// when
		warehouseService.list(null, pageable);

		// then: "IN ()" would be invalid SQL
		verify(warehouseRepository, never()).countSkus(any());
	}

	@Test
	void get_unknownId_throwsNotFound() {
		// given
		when(warehouseRepository.findById(9L)).thenReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> warehouseService.get(9L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void create_trimsTextAndStoresBlankAddressAsNull() {
		// given
		when(warehouseRepository.existsByCode("WH-03")).thenReturn(false);
		when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		WarehouseResponse created = warehouseService.create(new WarehouseRequest(" WH-03 ", " South Hub ", "  "));

		// then
		ArgumentCaptor<Warehouse> saved = ArgumentCaptor.forClass(Warehouse.class);
		verify(warehouseRepository).save(saved.capture());
		assertThat(saved.getValue().getCode()).isEqualTo("WH-03");
		assertThat(saved.getValue().getName()).isEqualTo("South Hub");
		assertThat(saved.getValue().getAddress()).isNull();
		assertThat(created.skuCount()).isZero();
	}

	@Test
	void create_codeTaken_throwsDuplicate() {
		// given
		when(warehouseRepository.existsByCode("WH-01")).thenReturn(true);

		// when / then
		assertThatThrownBy(() -> warehouseService.create(new WarehouseRequest("WH-01", "Copy", null)))
				.isInstanceOfSatisfying(DuplicateResourceException.class, ex -> {
					assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.WAREHOUSE_CODE_DUPLICATE);
					assertThat(ex.getMessageArgs()).containsExactly("WH-01");
				});
		verify(warehouseRepository, never()).save(any());
	}

	@Test
	void update_changesEveryFieldAndFlushes() {
		// given
		Warehouse main = warehouse(1L, "WH-01", "Main", "Old address");
		when(warehouseRepository.findById(1L)).thenReturn(Optional.of(main));
		when(warehouseRepository.existsByCodeAndIdNot("WH-10", 1L)).thenReturn(false);
		when(warehouseRepository.countSkus(List.of(1L))).thenReturn(List.of(skuCount(1L, 5)));

		// when
		WarehouseResponse updated = warehouseService.update(1L, new WarehouseRequest("WH-10", "Main Warehouse", null));

		// then
		assertThat(updated.code()).isEqualTo("WH-10");
		assertThat(updated.name()).isEqualTo("Main Warehouse");
		assertThat(updated.address()).isNull();
		assertThat(updated.skuCount()).isEqualTo(5);
		verify(warehouseRepository).flush();
	}

	@Test
	void update_codeOfAnotherWarehouse_throwsDuplicate() {
		// given
		when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, "WH-01", "Main", null)));
		when(warehouseRepository.existsByCodeAndIdNot("WH-02", 1L)).thenReturn(true);

		// when / then
		assertThatThrownBy(() -> warehouseService.update(1L, new WarehouseRequest("WH-02", "Main", null)))
				.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	void delete_withoutStock_deletes() {
		// given
		Warehouse empty = warehouse(1L, "WH-01", "Main", null);
		when(warehouseRepository.findById(1L)).thenReturn(Optional.of(empty));
		when(warehouseRepository.countSkus(List.of(1L))).thenReturn(List.of());

		// when
		warehouseService.delete(1L);

		// then
		verify(warehouseRepository).delete(empty);
	}

	@Test
	void delete_withStock_throwsInUse() {
		// given
		when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, "WH-01", "Main", null)));
		when(warehouseRepository.countSkus(List.of(1L))).thenReturn(List.of(skuCount(1L, 3)));

		// when / then
		assertThatThrownBy(() -> warehouseService.delete(1L))
				.isInstanceOfSatisfying(ResourceInUseException.class, ex -> {
					assertThat(ex.getMessageKey()).isEqualTo(MessageKeys.WAREHOUSE_DELETE_HAS_STOCK);
					assertThat(ex.getMessageArgs()).containsExactly("WH-01");
				});
		verify(warehouseRepository, never()).delete(any(Warehouse.class));
	}

	private static WarehouseSkuCount skuCount(long warehouseId, long count) {
		return new WarehouseSkuCount() {
			@Override
			public long getWarehouseId() {
				return warehouseId;
			}

			@Override
			public long getSkuCount() {
				return count;
			}
		};
	}
}
