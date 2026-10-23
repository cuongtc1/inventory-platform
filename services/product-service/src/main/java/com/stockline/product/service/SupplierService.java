package com.stockline.product.service;

import com.stockline.product.dto.PageResponse;
import com.stockline.product.dto.SupplierRequest;
import com.stockline.product.dto.SupplierResponse;
import com.stockline.product.entity.Supplier;
import com.stockline.product.exception.DuplicateResourceException;
import com.stockline.product.exception.ResourceInUseException;
import com.stockline.product.exception.ResourceNotFoundException;
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

		Supplier saved = supplierRepository.save(new Supplier(
				name, blankToNull(request.contactName()), blankToNull(request.email()), blankToNull(request.phone())));
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
		supplier.setContactName(blankToNull(request.contactName()));
		supplier.setEmail(blankToNull(request.email()));
		supplier.setPhone(blankToNull(request.phone()));
		// Run the UPDATE now so @PreUpdate sets updatedAt before we build the response
		supplierRepository.flush();
		return supplierMapper.toResponse(supplier);
	}

	@Transactional
	public void delete(Long id) {
		Supplier supplier = findSupplier(id);
		if (productRepository.existsBySupplierId(id)) {
			throw new ResourceInUseException(
					"Supplier '" + supplier.getName() + "' still has products; move or delete them first");
		}
		supplierRepository.delete(supplier);
	}

	private Supplier findSupplier(Long id) {
		return supplierRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
	}

	/**
	 * " Anna " becomes "Anna"; null, "" and "   " become null, so the table never holds empty strings.
	 */
	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	private static DuplicateResourceException duplicateName(String name) {
		return new DuplicateResourceException("Supplier '" + name + "' already exists");
	}
}
