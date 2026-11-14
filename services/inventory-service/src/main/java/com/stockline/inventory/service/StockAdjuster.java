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
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * The database part of an adjustment: the stock level and its movement are saved together or not at all. A separate
 * bean from {@link StockLevelService}, because the call to product-service must stay outside the transaction.
 *
 * <p>Retry outside, transaction inside: {@code @Retryable} wraps the whole method, and each attempt runs in a
 * transaction of its own ({@link TransactionOperations#execute}). A retry inside a failed transaction would be useless:
 * it would still see the old numbers, and the transaction is already marked for rollback.
 */
@Component
public class StockAdjuster {

	private final StockLevelRepository stockLevelRepository;
	private final StockMovementRepository stockMovementRepository;
	private final WarehouseRepository warehouseRepository;
	private final TransactionOperations transaction;

	public StockAdjuster(StockLevelRepository stockLevelRepository, StockMovementRepository stockMovementRepository,
						 WarehouseRepository warehouseRepository, TransactionOperations transaction) {
		this.stockLevelRepository = stockLevelRepository;
		this.stockMovementRepository = stockMovementRepository;
		this.warehouseRepository = warehouseRepository;
		this.transaction = transaction;
	}

	/**
	 * Two managers adjusting the same stock at once both succeed: the one that conflicts is tried again on the new
	 * numbers, instead of answering 409. Only after all retries does the caller see 409 CONCURRENT_UPDATE.
	 */
	@Retryable(includes = ConcurrencyFailureException.class, maxRetries = StockRetry.MAX_RETRIES,
			delay = StockRetry.DELAY_MS, jitter = StockRetry.JITTER_MS, multiplier = StockRetry.MULTIPLIER,
			maxDelay = StockRetry.MAX_DELAY_MS)
	public StockLevel apply(AdjustStockRequest request, ProductSnapshot product) {
		return transaction.execute(status -> applyOnce(request, product));
	}

	private StockLevel applyOnce(AdjustStockRequest request, ProductSnapshot product) {
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
		// conflict fails here, inside the attempt
		stockLevelRepository.flush();
		return saved;
	}
}
