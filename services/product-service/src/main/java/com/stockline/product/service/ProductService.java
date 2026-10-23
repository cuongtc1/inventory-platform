package com.stockline.product.service;

import com.stockline.product.constant.MessageKeys;
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
import com.stockline.product.util.MoneyUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
				MoneyUtils.toMoney(request.costPrice()), MoneyUtils.toMoney(request.salePrice()), request.reorderLevel(),
				request.status()));
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
		product.setCostPrice(MoneyUtils.toMoney(request.costPrice()));
		product.setSalePrice(MoneyUtils.toMoney(request.salePrice()));
		product.setReorderLevel(request.reorderLevel());
		product.setStatus(request.status());
		// Run the UPDATE now so JPA auditing sets updatedAt / updatedBy before we build the response
		productRepository.flush();
		return productMapper.toResponse(product);
	}

	/**
	 * Hard delete, the project rule for every resource (docs/conventions.md section 5). Other services refer to
	 * products only by id and keep their own copies of what they need (e.g. the product name on an order line),
	 * so their history stays readable after the product is gone.
	 */
	@Transactional
	public void delete(Long id) {
		productRepository.delete(findProduct(id));
	}

	private Product findProduct(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(Product.class.getSimpleName(), id));
	}

	private Category findCategory(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(Category.class.getSimpleName(), id));
	}

	private Supplier findSupplier(Long id) {
		if (id == null) {
			return null;
		}
		return supplierRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(Supplier.class.getSimpleName(), id));
	}

	private static DuplicateResourceException duplicateSku(String sku) {
		return new DuplicateResourceException(MessageKeys.PRODUCT_SKU_DUPLICATE, sku);
	}
}
