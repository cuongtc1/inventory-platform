package com.stockline.product.service;

import com.stockline.product.dto.PageResponse;
import com.stockline.product.dto.ProductFilter;
import com.stockline.product.dto.ProductRequest;
import com.stockline.product.dto.ProductResponse;
import com.stockline.product.entity.Category;
import com.stockline.product.entity.Product;
import com.stockline.product.entity.Supplier;
import com.stockline.product.exception.DuplicateResourceException;
import com.stockline.product.exception.ResourceNotFoundException;
import com.stockline.product.mapper.ProductMapper;
import com.stockline.product.repository.CategoryRepository;
import com.stockline.product.repository.ProductRepository;
import com.stockline.product.repository.ProductSpecifications;
import com.stockline.product.repository.SupplierRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Business rules for products:
 * <ul>
 *   <li>SKUs are unique (ignoring case);</li>
 *   <li>the category must exist; the supplier is optional but must exist when given.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final SupplierRepository supplierRepository;
	private final ProductMapper productMapper;

	public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
			SupplierRepository supplierRepository, ProductMapper productMapper) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.supplierRepository = supplierRepository;
		this.productMapper = productMapper;
	}

	public PageResponse<ProductResponse> list(ProductFilter filter, Pageable pageable) {
		Specification<Product> spec = ProductSpecifications.matching(
				filter.search(), filter.categoryId(), filter.supplierId(), filter.status());
		return PageResponse.from(productRepository.findAll(spec, pageable).map(productMapper::toResponse));
	}

	public ProductResponse get(Long id) {
		return productMapper.toResponse(findProduct(id));
	}

	@Transactional
	public ProductResponse create(ProductRequest request) {
		if (productRepository.existsBySku(request.sku())) {
			throw duplicateSku(request.sku());
		}
		Category category = findCategory(request.categoryId());
		Supplier supplier = findSupplier(request.supplierId());

		Product saved = productRepository.save(new Product(request.sku(), request.name().strip(), category, supplier,
				money(request.costPrice()), money(request.salePrice()), request.reorderLevel(), request.status()));
		return productMapper.toResponse(saved);
	}

	@Transactional
	public ProductResponse update(Long id, ProductRequest request) {
		Product product = findProduct(id);
		if (productRepository.existsBySkuAndIdNot(request.sku(), id)) {
			throw duplicateSku(request.sku());
		}
		Category category = findCategory(request.categoryId());
		Supplier supplier = findSupplier(request.supplierId());

		product.setSku(request.sku());
		product.setName(request.name().strip());
		product.setCategory(category);
		product.setSupplier(supplier);
		product.setCostPrice(money(request.costPrice()));
		product.setSalePrice(money(request.salePrice()));
		product.setReorderLevel(request.reorderLevel());
		product.setStatus(request.status());
		// Run the UPDATE now so @PreUpdate sets updatedAt before we build the response
		productRepository.flush();
		return productMapper.toResponse(product);
	}

	/**
	 * Hard delete. Other services refer to products only by id (no foreign keys across schemas), so nothing
	 * stops this here; once stock and orders exist, setting the status to INACTIVE is the safer choice.
	 */
	@Transactional
	public void delete(Long id) {
		productRepository.delete(findProduct(id));
	}

	private Product findProduct(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));
	}

	private Category findCategory(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category", id));
	}

	private Supplier findSupplier(Long id) {
		if (id == null) {
			return null;
		}
		return supplierRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
	}

	/**
	 * 40 becomes 40.00, like the DECIMAL(12,2) column, so the response to a save matches a later read.
	 * Never rounds: @Digits(fraction = 2) on the request already rejects a third decimal.
	 */
	private static BigDecimal money(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.UNNECESSARY);
	}

	private static DuplicateResourceException duplicateSku(String sku) {
		return new DuplicateResourceException("Product with SKU '" + sku + "' already exists");
	}
}
