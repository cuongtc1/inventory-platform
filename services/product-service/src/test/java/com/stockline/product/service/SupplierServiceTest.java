package com.stockline.product.service;

import com.stockline.common.constant.CommonMessageKeys;
import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.DuplicateResourceException;
import com.stockline.common.exception.ResourceInUseException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.product.constant.MessageKeys;
import com.stockline.product.dto.SupplierRequest;
import com.stockline.product.dto.SupplierResponse;
import com.stockline.product.entity.Supplier;
import com.stockline.product.mapper.SupplierMapper;
import com.stockline.product.repository.ProductRepository;
import com.stockline.product.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.stockline.product.TestData.supplier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test: no Spring, no database. Repositories are Mockito mocks; the mapper is the real class.
 */
@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

	@Mock
	private SupplierRepository supplierRepository;

	@Mock
	private ProductRepository productRepository;

	private SupplierService supplierService;

	@BeforeEach
	void setUp() {
		supplierService = new SupplierService(supplierRepository, productRepository, new SupplierMapper());
	}

	@Test
	void list_searchWithSpaces_trimsItAndMapsPage() {
		// given
		Pageable pageable = PageRequest.of(0, 20);
		Supplier logiCo = supplier(1L, "LogiCo", "Anna Pham", "anna@logico.com", null);
		when(supplierRepository.findByNameContainingIgnoreCase("logi", pageable))
				.thenReturn(new PageImpl<>(List.of(logiCo), pageable, 1));

		// when
		PageResponse<SupplierResponse> page = supplierService.list(" logi  ", pageable);

		// then
		assertThat(page.totalElements()).isEqualTo(1);
		assertThat(page.content().getFirst().name()).isEqualTo("LogiCo");
		assertThat(page.content().getFirst().contactName()).isEqualTo("Anna Pham");
	}

	@Test
	void list_noSearch_searchesWithEmptyText() {
		// given
		Pageable pageable = PageRequest.of(0, 20);
		when(supplierRepository.findByNameContainingIgnoreCase("", pageable))
				.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		// when
		PageResponse<SupplierResponse> page = supplierService.list(null, pageable);

		// then
		assertThat(page.content()).isEmpty();
		verify(supplierRepository).findByNameContainingIgnoreCase("", pageable);
	}

	@Test
	void get_existingId_returnsSupplier() {
		// given
		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier(1L, "LogiCo", null, null, null)));

		// when
		SupplierResponse response = supplierService.get(1L);

		// then
		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.name()).isEqualTo("LogiCo");
	}

	@Test
	void get_unknownId_throwsResourceNotFoundException() {
		// given
		when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> supplierService.get(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage(CommonMessageKeys.RESOURCE_NOT_FOUND)
				.extracting("messageArgs").isEqualTo(new Object[] {"Supplier", 99L});
	}

	@Test
	void create_newName_trimsFieldsAndStoresBlankAsNull() {
		// given
		when(supplierRepository.existsByName("LogiCo")).thenReturn(false);
		when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		SupplierResponse response = supplierService.create(
				new SupplierRequest(" LogiCo ", "  Anna Pham ", "", "   "));

		// then
		assertThat(response.name()).isEqualTo("LogiCo");
		assertThat(response.contactName()).isEqualTo("Anna Pham");
		assertThat(response.email()).isNull();
		assertThat(response.phone()).isNull();
	}

	@Test
	void create_duplicateName_throwsDuplicateResourceException() {
		// given
		when(supplierRepository.existsByName("LogiCo")).thenReturn(true);

		// when / then
		assertThatThrownBy(() -> supplierService.create(new SupplierRequest("LogiCo", null, null, null)))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage(MessageKeys.SUPPLIER_NAME_DUPLICATE)
				.extracting("messageArgs").isEqualTo(new Object[] {"LogiCo"});
		verify(supplierRepository, never()).save(any());
	}

	@Test
	void update_validRequest_replacesAllFields() {
		// given
		Supplier logiCo = supplier(1L, "Logico", "Anna Pham", "anna@logico.com", "+84 90 111 2233");
		when(supplierRepository.findById(1L)).thenReturn(Optional.of(logiCo));
		when(supplierRepository.existsByNameAndIdNot("LogiCo", 1L)).thenReturn(false);

		// when: PUT replaces every field, so a missing phone clears it
		SupplierResponse response = supplierService.update(1L,
				new SupplierRequest("LogiCo", "Tom Lee", "tom@logico.com", null));

		// then
		assertThat(response.name()).isEqualTo("LogiCo");
		assertThat(response.contactName()).isEqualTo("Tom Lee");
		assertThat(response.email()).isEqualTo("tom@logico.com");
		assertThat(response.phone()).isNull();
		verify(supplierRepository).flush();
	}

	@Test
	void update_duplicateName_throwsDuplicateResourceException() {
		// given
		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier(1L, "LogiCo", null, null, null)));
		when(supplierRepository.existsByNameAndIdNot("WireCo", 1L)).thenReturn(true);

		// when / then
		assertThatThrownBy(() -> supplierService.update(1L, new SupplierRequest("WireCo", null, null, null)))
				.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	void update_unknownId_throwsResourceNotFoundException() {
		// given
		when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> supplierService.update(99L, new SupplierRequest("LogiCo", null, null, null)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void delete_unusedSupplier_deletesIt() {
		// given
		Supplier logiCo = supplier(1L, "LogiCo", null, null, null);
		when(supplierRepository.findById(1L)).thenReturn(Optional.of(logiCo));
		when(productRepository.existsBySupplierId(1L)).thenReturn(false);

		// when
		supplierService.delete(1L);

		// then
		verify(supplierRepository).delete(logiCo);
	}

	@Test
	void delete_withProducts_throwsResourceInUseException() {
		// given
		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier(1L, "LogiCo", null, null, null)));
		when(productRepository.existsBySupplierId(1L)).thenReturn(true);

		// when / then
		assertThatThrownBy(() -> supplierService.delete(1L))
				.isInstanceOf(ResourceInUseException.class)
				.hasMessage(MessageKeys.SUPPLIER_DELETE_HAS_PRODUCTS)
				.extracting("messageArgs").isEqualTo(new Object[] {"LogiCo"});
		verify(supplierRepository, never()).delete(any());
	}

}
