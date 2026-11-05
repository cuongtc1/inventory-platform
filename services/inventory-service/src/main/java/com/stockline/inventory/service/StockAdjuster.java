package com.stockline.inventory.service;

import com.stockline.common.exception.InsufficientStockException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.common.util.TextUtils;
import com.stockline.inventory.constant.MessageKeys;
import com.stockline.inventory.constant.MovementType;
import com.stockline.inventory.dto.AdjustStockRequest;
import com.stockline.inventory.dto.ProductSnapshot;
import com.stockline.inventory.entity.StockLevel;
import com.stockline.inventory.entity.StockMovement;
import com.stockline.inventory.entity.Warehouse;
import com.stockline.inventory.repository.StockLevelRepository;
import com.stockline.inventory.repository.StockMovementRepository;
import com.stockline.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The database part of an adjustment, in one transaction: the stock level and its movement are saved together or not
 * at all. A separate bean from {@link StockLevelService}, because {@code @Transactional} only works on calls from
 * another bean, and the call to product-service must stay outside the transaction.
 */
@Component
public class StockAdjuster {

	private final StockLevelRepository stockLevelRepository;
	private final StockMovementRepository stockMovementRepository;
	private final WarehouseRepository warehouseRepository;

	public StockAdjuster(StockLevelRepository stockLevelRepository, StockMovementRepository stockMovementRepository,
						 WarehouseRepository warehouseRepository) {
		this.stockLevelRepository = stockLevelRepository;
		this.stockMovementRepository = stockMovementRepository;
		this.warehouseRepository = warehouseRepository;
	}

	@Transactional
	public StockLevel apply(AdjustStockRequest request, ProductSnapshot product) {
		Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
				.orElseThrow(() -> new ResourceNotFoundException(Warehouse.class.getSimpleName(), request.warehouseId()));
		StockLevel level = stockLevelRepository.findByWarehouseIdAndProductId(warehouse.getId(), product.id())
				.orElseGet(() -> new StockLevel(warehouse, product.id()));
		level.copyProduct(product.sku(), product.name(), product.reorderLevel());

		// Reserved stock is promised to orders, so an adjustment can take only what is available
		if (level.getAvailable() + request.quantity() < 0) {
			throw new InsufficientStockException(MessageKeys.STOCK_INSUFFICIENT, product.name(), warehouse.getCode(),
					level.getAvailable());
		}
		level.setOnHand(level.getOnHand() + request.quantity());

		StockLevel saved = stockLevelRepository.save(level);
		stockMovementRepository.save(new StockMovement(saved, MovementType.ADJUST, request.quantity(),
				TextUtils.blankToNull(request.reference())));
		// Run the SQL now so JPA auditing sets updatedAt / updatedBy before the response is built, and so a version
		// conflict fails here
		stockLevelRepository.flush();
		return saved;
	}
}
