package com.stockline.inventory.service;

import com.stockline.common.constant.CommonMessageKeys;
import com.stockline.inventory.constant.MessageKeys;
import com.stockline.inventory.constant.MovementType;
import com.stockline.inventory.entity.StockLevel;
import com.stockline.inventory.entity.StockMovement;
import com.stockline.inventory.entity.StockReservation;
import com.stockline.inventory.entity.Warehouse;
import com.stockline.inventory.event.OrderCreatedEvent;
import com.stockline.inventory.repository.StockLevelRepository;
import com.stockline.inventory.repository.StockMovementRepository;
import com.stockline.inventory.repository.StockReservationRepository;
import com.stockline.inventory.repository.WarehouseRepository;
import org.springframework.context.MessageSource;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Reserves the stock of a new order: all lines or none. Every line is checked first; only when all of them fit is
 * anything changed, so a failed order leaves no half reservation behind.
 *
 * <p>Two orders for the last items, handled at the same moment (listener threads, or two instances of the service), both
 * read the same stock level. The first to save wins; the other gets a version conflict and is tried again (T6.3), now
 * with the winner's numbers, so it reserves the rest or fails. Never more than is on hand: the @Version check, and
 * behind it the CHECK constraint reserved <= on_hand.
 */
@Service
public class StockReserver {

	/**
	 * What reserving gave. A {@code sealed} interface (Java 17): these two records are the only results, so a
	 * {@code switch} over a result handles both and needs no {@code default}.
	 */
	public sealed interface Result permits Reserved, Failed {
	}

	public record Reserved(long orderId) implements Result {
	}

	public record Failed(long orderId, String reason) implements Result {
	}

	private final StockLevelRepository stockLevelRepository;
	private final StockMovementRepository stockMovementRepository;
	private final StockReservationRepository stockReservationRepository;
	private final WarehouseRepository warehouseRepository;
	private final MessageSource messageSource;
	private final TransactionOperations transaction;

	public StockReserver(StockLevelRepository stockLevelRepository, StockMovementRepository stockMovementRepository,
						 StockReservationRepository stockReservationRepository, WarehouseRepository warehouseRepository,
						 MessageSource messageSource, TransactionOperations transaction) {
		this.stockLevelRepository = stockLevelRepository;
		this.stockMovementRepository = stockMovementRepository;
		this.stockReservationRepository = stockReservationRepository;
		this.warehouseRepository = warehouseRepository;
		this.messageSource = messageSource;
		this.transaction = transaction;
	}

	/**
	 * In one transaction: raise {@code reserved} on each stock level, store a reservation per line and a RESERVE
	 * movement (referenced by the order number). {@code on_hand} doesn't change: the goods are still in the warehouse.
	 *
	 * <p>Retry outside, transaction inside (like StockAdjuster): a version conflict or a deadlock rolls the attempt
	 * back, and the next attempt starts a new transaction that reads the stock again. Only after all retries does the
	 * exception reach the listener, and Kafka delivers the message again.
	 */
	@Retryable(includes = ConcurrencyFailureException.class, maxRetries = StockRetry.MAX_RETRIES,
			delay = StockRetry.DELAY_MS, jitter = StockRetry.JITTER_MS, multiplier = StockRetry.MULTIPLIER,
			maxDelay = StockRetry.MAX_DELAY_MS)
	public Result reserve(OrderCreatedEvent order) {
		return transaction.execute(status -> reserveOnce(order));
	}

	private Result reserveOnce(OrderCreatedEvent order) {
		Optional<Warehouse> warehouse = warehouseRepository.findById(order.warehouseId());
		if (warehouse.isEmpty()) {
			return new Failed(order.orderId(), text(CommonMessageKeys.RESOURCE_NOT_FOUND, "Warehouse", order.warehouseId()));
		}

		// First check every line; nothing changes unless all of them fit
		List<StockLevel> levels = new ArrayList<>();
		for (OrderCreatedEvent.Item item : order.items()) {
			StockLevel level = stockLevelRepository.findByWarehouseIdAndProductId(order.warehouseId(), item.productId())
					.orElse(null);
			// No stock level: this warehouse has never had the product, so nothing is available
			int available = level == null ? 0 : level.getAvailable();
			if (available < item.quantity()) {
				String name = level == null ? item.productName() : level.getProductName();
				return new Failed(order.orderId(),
						text(MessageKeys.STOCK_INSUFFICIENT, name, warehouse.get().getCode(), available));
			}
			levels.add(level);
		}

		for (int i = 0; i < levels.size(); i++) {
			StockLevel level = levels.get(i);
			int quantity = order.items().get(i).quantity();
			level.reserve(quantity);
			stockReservationRepository.save(new StockReservation(order.orderId(), level, quantity));
			stockMovementRepository.save(new StockMovement(level, MovementType.RESERVE, quantity, order.orderNumber()));
		}
		// Run the SQL now: a version conflict or a duplicate reservation fails inside this method
		stockLevelRepository.flush();
		return new Reserved(order.orderId());
	}

	private String text(String key, Object... args) {
		return messageSource.getMessage(key, args, Locale.ROOT);
	}
}
