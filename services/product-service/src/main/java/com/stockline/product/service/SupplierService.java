package com.stockline.product.service;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.DuplicateResourceException;
import com.stockline.common.exception.ResourceInUseException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.common.util.TextUtils;
import com.stockline.product.constant.MessageKeys;
import com.stockline.product.dto.SupplierRequest;
import com.stockline.product.dto.SupplierResponse;
import com.stockline.product.entity.Supplier;
import com.stockline.product.mapper.SupplierMapper;
import com.stockline.product.repository.ProductRepository;
import com.stockline.product.repository.SupplierRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for suppliers:
 * <ul>
 *   <li>names are unique (ignoring case);</li>
 *   <li>text fields are trimmed, and empty optional fields are stored as null;</li>
 *   <li>a supplier with products can't be deleted.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class SupplierService {

	private final SupplierRepository supplierRepository;
	private final ProductRepository productRepository;
	private final SupplierMapper supplierMapper;

	public SupplierService(SupplierRepository supplierRepository, ProductRepository productRepository,
						   SupplierMapper supplierMapper) {
		this.supplierRepository = supplierRepository;
		this.productRepository = productRepository;
		this.supplierMapper = supplierMapper;
	}

	public PageResponse<SupplierResponse> list(String search, Pageable pageable) {
		String text = search == null ? "" : search.strip();
		return PageResponse.from(
				supplierRepository.findByNameContainingIgnoreCase(text, pageable).map(supplierMapper::toResponse));
	}

	public SupplierResponse get(Long id) {
		return supplierMapper.toResponse(findSupplier(id));
	}

	@Transactional
	public SupplierResponse create(SupplierRequest request) {
		String name = request.name().strip();
		if (supplierRepository.existsByName(name)) {
			throw duplicateName(name);
		}

		Supplier saved = supplierRepository.save(new Supplier(name, TextUtils.blankToNull(request.contactName()),
				TextUtils.blankToNull(request.email()), TextUtils.blankToNull(request.phone())));
		return supplierMapper.toResponse(saved);
	}

	@Transactional
	public SupplierResponse update(Long id, SupplierRequest request) {
		Supplier supplier = findSupplier(id);
		String name = request.name().strip();
		if (supplierRepository.existsByNameAndIdNot(name, id)) {
			throw duplicateName(name);
		}

		supplier.setName(name);
		supplier.setContactName(TextUtils.blankToNull(request.contactName()));
		supplier.setEmail(TextUtils.blankToNull(request.email()));
		supplier.setPhone(TextUtils.blankToNull(request.phone()));
		// Run the UPDATE now so JPA auditing sets updatedAt / updatedBy before we build the response
		supplierRepository.flush();
		return supplierMapper.toResponse(supplier);
	}

	@Transactional
	public void delete(Long id) {
		Supplier supplier = findSupplier(id);
		if (productRepository.existsBySupplierId(id)) {
			throw new ResourceInUseException(MessageKeys.SUPPLIER_DELETE_HAS_PRODUCTS, supplier.getName());
		}
		supplierRepository.delete(supplier);
	}

	private Supplier findSupplier(Long id) {
		return supplierRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(Supplier.class.getSimpleName(), id));
	}

	private static DuplicateResourceException duplicateName(String name) {
		return new DuplicateResourceException(MessageKeys.SUPPLIER_NAME_DUPLICATE, name);
	}
}
