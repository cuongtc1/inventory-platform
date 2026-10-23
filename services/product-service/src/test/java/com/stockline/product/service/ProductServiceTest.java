package com.stockline.product.service;

import com.stockline.product.dto.PageResponse;
import com.stockline.product.dto.ProductFilter;
import com.stockline.product.dto.ProductRequest;
import com.stockline.product.dto.ProductResponse;
import com.stockline.product.entity.Category;
import com.stockline.product.entity.Product;
import com.stockline.product.entity.ProductStatus;
import com.stockline.product.entity.Supplier;
import com.stockline.product.exception.DuplicateResourceException;
import com.stockline.product.exception.ResourceNotFoundException;
import com.stockline.product.mapper.ProductMapper;
import com.stockline.product.message.MessageKeys;
import com.stockline.product.repository.CategoryRepository;
import com.stockline.product.repository.ProductRepository;
import com.stockline.product.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test: no Spring, no database. Repositories are Mockito mocks; the mapper is the real class.
 * The filters themselves are tested against MySQL in ProductSpecificationsTest.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private SupplierRepository supplierRepository;

	private ProductService productService;

	private Category peripherals;
	private Supplier logiCo;

	@BeforeEach
	void setUp() {
		productService = new ProductService(productRepository, categoryRepository, supplierRepository,
				new ProductMapper());
		peripherals = withId(new Category("Peripherals", null), 3L);
		logiCo = withId(new Supplier("LogiCo", null, null, null), 5L);
	}

	@Test
	void list_anyFilter_returnsMappedPage() {
		// given
		Pageable pageable = PageRequest.of(0, 20);
		Product mouse = withId(mouse(peripherals, logiCo), 12L);
		when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>any(), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(mouse), pageable, 1));

		// when
		PageResponse<ProductResponse> page = productService.list(
				new ProductFilter("mouse", 3L, null, ProductStatus.ACTIVE), pageable);

		// then
		assertThat(page.totalElements()).isEqualTo(1);
		assertThat(page.content().getFirst().categoryName()).isEqualTo("Peripherals");
		assertThat(page.content().getFirst().supplierName()).isEqualTo("LogiCo");
	}

	@Test
	void get_unknownId_throwsResourceNotFoundException() {
		// given
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> productService.get(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage(MessageKeys.RESOURCE_NOT_FOUND);
	}

	@Test
	void create_validRequest_savesProductWithCategoryAndSupplier() {
		// given
		when(productRepository.existsBySku("A01")).thenReturn(false);
		when(categoryRepository.findById(3L)).thenReturn(Optional.of(peripherals));
		when(supplierRepository.findById(5L)).thenReturn(Optional.of(logiCo));
		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		ProductResponse response = productService.create(request("A01", " Wireless Mouse ", 3L, 5L));

		// then
		assertThat(response.sku()).isEqualTo("A01");
		assertThat(response.name()).isEqualTo("Wireless Mouse");
		assertThat(response.categoryId()).isEqualTo(3L);
		assertThat(response.supplierName()).isEqualTo("LogiCo");
		assertThat(response.salePrice()).isEqualByComparingTo("12.00");
		assertThat(response.status()).isEqualTo(ProductStatus.ACTIVE);
	}

	@Test
	void create_withoutSupplier_savesProductWithNullSupplier() {
		// given
		when(productRepository.existsBySku("A01")).thenReturn(false);
		when(categoryRepository.findById(3L)).thenReturn(Optional.of(peripherals));
		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		ProductResponse response = productService.create(request("A01", "Wireless Mouse", 3L, null));

		// then
		assertThat(response.supplierId()).isNull();
		assertThat(response.supplierName()).isNull();
		verify(supplierRepository, never()).findById(any());
	}

	@Test
	void create_duplicateSku_throwsDuplicateResourceException() {
		// given
		when(productRepository.existsBySku("A01")).thenReturn(true);

		// when / then
		assertThatThrownBy(() -> productService.create(request("A01", "Wireless Mouse", 3L, null)))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage(MessageKeys.PRODUCT_SKU_DUPLICATE);
		verify(productRepository, never()).save(any());
	}

	@Test
	void create_unknownCategory_throwsResourceNotFoundException() {
		// given
		when(productRepository.existsBySku("A01")).thenReturn(false);
		when(categoryRepository.findById(42L)).thenReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> productService.create(request("A01", "Wireless Mouse", 42L, null)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage(MessageKeys.RESOURCE_NOT_FOUND);
		verify(productRepository, never()).save(any());
	}

	@Test
	void create_unknownSupplier_throwsResourceNotFoundException() {
		// given
		when(productRepository.existsBySku("A01")).thenReturn(false);
		when(categoryRepository.findById(3L)).thenReturn(Optional.of(peripherals));
		when(supplierRepository.findById(42L)).thenReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> productService.create(request("A01", "Wireless Mouse", 3L, 42L)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage(MessageKeys.RESOURCE_NOT_FOUND);
		verify(productRepository, never()).save(any());
	}

	@Test
	void update_validRequest_replacesAllFields() {
		// given: the product has a supplier; the update moves it to another category and removes the supplier
		Product mouse = withId(mouse(peripherals, logiCo), 12L);
		Category accessories = withId(new Category("Accessories", null), 4L);
		when(productRepository.findById(12L)).thenReturn(Optional.of(mouse));
		when(productRepository.existsBySkuAndIdNot("A01-B", 12L)).thenReturn(false);
		when(categoryRepository.findById(4L)).thenReturn(Optional.of(accessories));
		ProductRequest request = new ProductRequest("A01-B", "Silent Mouse", 4L, null,
				new BigDecimal("9"), new BigDecimal("14.5"), 30, ProductStatus.INACTIVE);

		// when
		ProductResponse response = productService.update(12L, request);

		// then
		assertThat(response.sku()).isEqualTo("A01-B");
		assertThat(response.name()).isEqualTo("Silent Mouse");
		assertThat(response.categoryName()).isEqualTo("Accessories");
		assertThat(response.supplierId()).isNull();
		// isEqualTo (not isEqualByComparingTo) also checks the scale: 9 is stored and returned as 9.00
		assertThat(response.costPrice()).isEqualTo(new BigDecimal("9.00"));
		assertThat(response.salePrice()).isEqualTo(new BigDecimal("14.50"));
		assertThat(response.reorderLevel()).isEqualTo(30);
		assertThat(response.status()).isEqualTo(ProductStatus.INACTIVE);
		verify(productRepository).flush();
	}

	@Test
	void update_skuOfAnotherProduct_throwsDuplicateResourceException() {
		// given
		when(productRepository.findById(12L)).thenReturn(Optional.of(withId(mouse(peripherals, logiCo), 12L)));
		when(productRepository.existsBySkuAndIdNot("A02", 12L)).thenReturn(true);

		// when / then
		assertThatThrownBy(() -> productService.update(12L, request("A02", "Wireless Mouse", 3L, null)))
				.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	void delete_existingProduct_deletesIt() {
		// given
		Product mouse = withId(mouse(peripherals, logiCo), 12L);
		when(productRepository.findById(12L)).thenReturn(Optional.of(mouse));

		// when
		productService.delete(12L);

		// then
		verify(productRepository).delete(mouse);
	}

	@Test
	void delete_unknownId_throwsResourceNotFoundException() {
		// given
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> productService.delete(99L))
				.isInstanceOf(ResourceNotFoundException.class);
		// any(Product.class), not any(): JpaSpecificationExecutor also has delete(PredicateSpecification)
		verify(productRepository, never()).delete(any(Product.class));
	}

	private static ProductRequest request(String sku, String name, Long categoryId, Long supplierId) {
		return new ProductRequest(sku, name, categoryId, supplierId,
				new BigDecimal("8.00"), new BigDecimal("12.00"), 20, ProductStatus.ACTIVE);
	}

	private static Product mouse(Category category, Supplier supplier) {
		return new Product("A01", "Wireless Mouse", category, supplier,
				new BigDecimal("8.00"), new BigDecimal("12.00"), 20, ProductStatus.ACTIVE);
	}

	/**
	 * The database normally sets the id; in a unit test we set the private field directly.
	 */
	private static <T> T withId(T entity, Long id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}
}
