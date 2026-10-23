package com.stockline.product.service;

import com.stockline.product.dto.CategoryRequest;
import com.stockline.product.dto.CategoryResponse;
import com.stockline.product.entity.Category;
import com.stockline.product.exception.DuplicateResourceException;
import com.stockline.product.exception.InvalidRequestException;
import com.stockline.product.exception.ResourceInUseException;
import com.stockline.product.exception.ResourceNotFoundException;
import com.stockline.product.mapper.CategoryMapper;
import com.stockline.product.repository.CategoryRepository;
import com.stockline.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

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
class CategoryServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private ProductRepository productRepository;

	private CategoryService categoryService;

	@BeforeEach
	void setUp() {
		categoryService = new CategoryService(categoryRepository, productRepository, new CategoryMapper());
	}

	@Test
	void get_unknownId_throwsResourceNotFoundException() {
		// given
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> categoryService.get(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Category with id 99 not found");
	}

	@Test
	void create_newNameWithParent_savesCategory() {
		// given
		Category electronics = category(1L, "Electronics", null);
		when(categoryRepository.existsByName("Cables")).thenReturn(false);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));
		when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		CategoryResponse response = categoryService.create(new CategoryRequest("  Cables ", 1L));

		// then: name is trimmed and the parent is set
		assertThat(response.name()).isEqualTo("Cables");
		assertThat(response.parentId()).isEqualTo(1L);
		assertThat(response.parentName()).isEqualTo("Electronics");
	}

	@Test
	void create_duplicateName_throwsDuplicateResourceException() {
		// given
		when(categoryRepository.existsByName("Cables")).thenReturn(true);

		// when / then
		assertThatThrownBy(() -> categoryService.create(new CategoryRequest("Cables", null)))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("Category 'Cables' already exists");
		verify(categoryRepository, never()).save(any());
	}

	@Test
	void create_unknownParent_throwsResourceNotFoundException() {
		// given
		when(categoryRepository.existsByName("Cables")).thenReturn(false);
		when(categoryRepository.findById(42L)).thenReturn(Optional.empty());

		// when / then
		assertThatThrownBy(() -> categoryService.create(new CategoryRequest("Cables", 42L)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Category with id 42 not found");
		verify(categoryRepository, never()).save(any());
	}

	@Test
	void update_validRequest_changesNameAndParent() {
		// given
		Category electronics = category(1L, "Electronics", null);
		Category cables = category(2L, "Cable", null);
		when(categoryRepository.findById(2L)).thenReturn(Optional.of(cables));
		when(categoryRepository.existsByNameAndIdNot("Cables", 2L)).thenReturn(false);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));

		// when
		CategoryResponse response = categoryService.update(2L, new CategoryRequest("Cables", 1L));

		// then
		assertThat(response.name()).isEqualTo("Cables");
		assertThat(response.parentName()).isEqualTo("Electronics");
		verify(categoryRepository).flush();
	}

	@Test
	void update_duplicateName_throwsDuplicateResourceException() {
		// given
		when(categoryRepository.findById(2L)).thenReturn(Optional.of(category(2L, "Cable", null)));
		when(categoryRepository.existsByNameAndIdNot("Displays", 2L)).thenReturn(true);

		// when / then
		assertThatThrownBy(() -> categoryService.update(2L, new CategoryRequest("Displays", null)))
				.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	void update_parentIsItself_throwsInvalidRequestException() {
		// given
		Category cables = category(2L, "Cables", null);
		when(categoryRepository.findById(2L)).thenReturn(Optional.of(cables));
		when(categoryRepository.existsByNameAndIdNot("Cables", 2L)).thenReturn(false);

		// when / then
		assertThatThrownBy(() -> categoryService.update(2L, new CategoryRequest("Cables", 2L)))
				.isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void update_parentIsOwnSubcategory_throwsInvalidRequestException() {
		// given: Electronics -> Cables -> USB Cables; try to move Electronics under USB Cables
		Category electronics = category(1L, "Electronics", null);
		Category cables = category(2L, "Cables", electronics);
		Category usbCables = category(3L, "USB Cables", cables);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(electronics));
		when(categoryRepository.existsByNameAndIdNot("Electronics", 1L)).thenReturn(false);
		when(categoryRepository.findById(3L)).thenReturn(Optional.of(usbCables));

		// when / then
		assertThatThrownBy(() -> categoryService.update(1L, new CategoryRequest("Electronics", 3L)))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("subcategories");
		assertThat(electronics.getParent()).isNull();
	}

	@Test
	void delete_unusedCategory_deletesIt() {
		// given
		Category cables = category(2L, "Cables", null);
		when(categoryRepository.findById(2L)).thenReturn(Optional.of(cables));
		when(categoryRepository.existsByParentId(2L)).thenReturn(false);
		when(productRepository.existsByCategoryId(2L)).thenReturn(false);

		// when
		categoryService.delete(2L);

		// then
		verify(categoryRepository).delete(cables);
	}

	@Test
	void delete_withSubcategories_throwsResourceInUseException() {
		// given
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Electronics", null)));
		when(categoryRepository.existsByParentId(1L)).thenReturn(true);

		// when / then
		assertThatThrownBy(() -> categoryService.delete(1L))
				.isInstanceOf(ResourceInUseException.class)
				.hasMessageContaining("subcategories");
		verify(categoryRepository, never()).delete(any());
	}

	@Test
	void delete_withProducts_throwsResourceInUseException() {
		// given
		when(categoryRepository.findById(2L)).thenReturn(Optional.of(category(2L, "Cables", null)));
		when(categoryRepository.existsByParentId(2L)).thenReturn(false);
		when(productRepository.existsByCategoryId(2L)).thenReturn(true);

		// when / then
		assertThatThrownBy(() -> categoryService.delete(2L))
				.isInstanceOf(ResourceInUseException.class)
				.hasMessageContaining("products");
		verify(categoryRepository, never()).delete(any());
	}

	/**
	 * The database normally sets the id; in a unit test we set the private field directly.
	 */
	private static Category category(Long id, String name, Category parent) {
		Category category = new Category(name, parent);
		ReflectionTestUtils.setField(category, "id", id);
		return category;
	}
}
