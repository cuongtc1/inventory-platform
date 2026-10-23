package com.stockline.product.service;

import com.stockline.product.constant.MessageKeys;
import com.stockline.product.dto.CategoryRequest;
import com.stockline.product.dto.CategoryResponse;
import com.stockline.product.dto.PageResponse;
import com.stockline.product.entity.Category;
import com.stockline.product.exception.DuplicateResourceException;
import com.stockline.product.exception.InvalidRequestException;
import com.stockline.product.exception.ResourceInUseException;
import com.stockline.product.exception.ResourceNotFoundException;
import com.stockline.product.mapper.CategoryMapper;
import com.stockline.product.repository.CategoryRepository;
import com.stockline.product.repository.ProductRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Business rules for categories:
 * <ul>
 *   <li>names are unique (ignoring case);</li>
 *   <li>the parent must exist, and a category can't be its own parent or sit under one of its subcategories;</li>
 *   <li>a category with subcategories or products can't be deleted.</li>
 * </ul>
 */
@Service
// Every public method runs in a read-only transaction unless it says otherwise
@Transactional(readOnly = true)
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;
	private final CategoryMapper categoryMapper;

	public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository,
						   CategoryMapper categoryMapper) {
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
		this.categoryMapper = categoryMapper;
	}

	public PageResponse<CategoryResponse> list(String search, Pageable pageable) {
		String text = search == null ? "" : search.strip();
		return PageResponse.from(
				categoryRepository.findByNameContainingIgnoreCase(text, pageable).map(categoryMapper::toResponse));
	}

	public CategoryResponse get(Long id) {
		return categoryMapper.toResponse(findCategory(id));
	}

	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		String name = request.name().strip();
		if (categoryRepository.existsByName(name)) {
			throw duplicateName(name);
		}
		Category parent = findParent(request.parentId());

		Category saved = categoryRepository.save(new Category(name, parent));
		return categoryMapper.toResponse(saved);
	}

	@Transactional
	public CategoryResponse update(Long id, CategoryRequest request) {
		Category category = findCategory(id);
		String name = request.name().strip();
		if (categoryRepository.existsByNameAndIdNot(name, id)) {
			throw duplicateName(name);
		}
		Category parent = findParent(request.parentId());
		checkNotOwnAncestor(category, parent);

		// No save() needed: the entity is managed, so JPA writes the changes when the transaction commits
		category.setName(name);
		category.setParent(parent);
		// Run the UPDATE now so JPA auditing sets updatedAt / updatedBy before we build the response
		categoryRepository.flush();
		return categoryMapper.toResponse(category);
	}

	@Transactional
	public void delete(Long id) {
		Category category = findCategory(id);
		if (categoryRepository.existsByParentId(id)) {
			throw new ResourceInUseException(MessageKeys.CATEGORY_DELETE_HAS_SUBCATEGORIES, category.getName());
		}
		if (productRepository.existsByCategoryId(id)) {
			throw new ResourceInUseException(MessageKeys.CATEGORY_DELETE_HAS_PRODUCTS, category.getName());
		}
		categoryRepository.delete(category);
	}

	private Category findCategory(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(Category.class.getSimpleName(), id));
	}

	private Category findParent(Long parentId) {
		return parentId == null ? null : findCategory(parentId);
	}

	/**
	 * Walks up from the new parent to the top. Meeting {@code category} on the way means the move would
	 * create a loop (A under B under A), which would make the tree endless.
	 */
	private static void checkNotOwnAncestor(Category category, Category newParent) {
		for (Category current = newParent; current != null; current = current.getParent()) {
			if (Objects.equals(current.getId(), category.getId())) {
				throw new InvalidRequestException(MessageKeys.CATEGORY_PARENT_LOOP);
			}
		}
	}

	private static DuplicateResourceException duplicateName(String name) {
		return new DuplicateResourceException(MessageKeys.CATEGORY_NAME_DUPLICATE, name);
	}
}
