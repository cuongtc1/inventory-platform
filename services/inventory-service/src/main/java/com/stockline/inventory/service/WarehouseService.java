package com.stockline.inventory.service;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.DuplicateResourceException;
import com.stockline.common.exception.ResourceInUseException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.common.util.TextUtils;
import com.stockline.inventory.constant.MessageKeys;
import com.stockline.inventory.dto.WarehouseRequest;
import com.stockline.inventory.dto.WarehouseResponse;
import com.stockline.inventory.entity.Warehouse;
import com.stockline.inventory.mapper.WarehouseMapper;
import com.stockline.inventory.repository.StockLevelRepository;
import com.stockline.inventory.repository.StockLevelRepository.WarehouseSkuCount;
import com.stockline.inventory.repository.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Warehouses. The rules: a code is unique ignoring case, and a warehouse with stock levels can't be deleted.
 */
@Service
@Transactional(readOnly = true)
public class WarehouseService {

	private final WarehouseRepository warehouseRepository;
	private final StockLevelRepository stockLevelRepository;
	private final WarehouseMapper warehouseMapper;

	public WarehouseService(WarehouseRepository warehouseRepository, StockLevelRepository stockLevelRepository,
							WarehouseMapper warehouseMapper) {
		this.warehouseRepository = warehouseRepository;
		this.stockLevelRepository = stockLevelRepository;
		this.warehouseMapper = warehouseMapper;
	}

	public PageResponse<WarehouseResponse> list(String search, Pageable pageable) {
		String text = search == null ? "" : search.strip();
		Page<Warehouse> page = warehouseRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(
				text, text, pageable);
		Map<Long, Long> skuCounts = skuCounts(page.getContent());
		return PageResponse.from(page.map(warehouse ->
				warehouseMapper.toResponse(warehouse, skuCounts.getOrDefault(warehouse.getId(), 0L))));
	}

	public WarehouseResponse get(Long id) {
		Warehouse warehouse = findWarehouse(id);
		return warehouseMapper.toResponse(warehouse, skuCount(warehouse));
	}

	@Transactional
	public WarehouseResponse create(WarehouseRequest request) {
		String code = request.code().strip();
		if (warehouseRepository.existsByCode(code)) {
			throw duplicateCode(code);
		}

		Warehouse saved = warehouseRepository.save(new Warehouse(code, request.name().strip(),
				TextUtils.blankToNull(request.address())));
		return warehouseMapper.toResponse(saved, 0);
	}

	@Transactional
	public WarehouseResponse update(Long id, WarehouseRequest request) {
		Warehouse warehouse = findWarehouse(id);
		String code = request.code().strip();
		if (warehouseRepository.existsByCodeAndIdNot(code, id)) {
			throw duplicateCode(code);
		}

		warehouse.setCode(code);
		warehouse.setName(request.name().strip());
		warehouse.setAddress(TextUtils.blankToNull(request.address()));
		// Run the UPDATE now so JPA auditing sets updatedAt / updatedBy before we build the response
		warehouseRepository.flush();
		return warehouseMapper.toResponse(warehouse, skuCount(warehouse));
	}

	/**
	 * Hard delete, the project rule. Movements and reservations always come with a stock level, so this one check
	 * covers them; if one slips through, its foreign key gives the common 409 IN_USE.
	 */
	@Transactional
	public void delete(Long id) {
		Warehouse warehouse = findWarehouse(id);
		if (skuCount(warehouse) > 0) {
			throw new ResourceInUseException(MessageKeys.WAREHOUSE_DELETE_HAS_STOCK, warehouse.getCode());
		}
		warehouseRepository.delete(warehouse);
	}

	private long skuCount(Warehouse warehouse) {
		return skuCounts(List.of(warehouse)).getOrDefault(warehouse.getId(), 0L);
	}

	private Map<Long, Long> skuCounts(List<Warehouse> warehouses) {
		if (warehouses.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = warehouses.stream().map(Warehouse::getId).toList();
		return stockLevelRepository.countByWarehouseIds(ids).stream()
				.collect(Collectors.toMap(WarehouseSkuCount::getWarehouseId, WarehouseSkuCount::getSkuCount));
	}

	private Warehouse findWarehouse(Long id) {
		return warehouseRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(Warehouse.class.getSimpleName(), id));
	}

	private static DuplicateResourceException duplicateCode(String code) {
		return new DuplicateResourceException(MessageKeys.WAREHOUSE_CODE_DUPLICATE, code);
	}
}
