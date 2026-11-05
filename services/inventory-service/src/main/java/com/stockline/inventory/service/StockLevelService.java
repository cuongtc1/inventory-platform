package com.stockline.inventory.service;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.InvalidRequestException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.inventory.client.ProductClient;
import com.stockline.inventory.constant.MessageKeys;
import com.stockline.inventory.dto.AdjustStockRequest;
import com.stockline.inventory.dto.ProductSnapshot;
import com.stockline.inventory.dto.StockLevelFilter;
import com.stockline.inventory.dto.StockLevelResponse;
import com.stockline.inventory.entity.StockLevel;
import com.stockline.inventory.mapper.StockLevelMapper;
import com.stockline.inventory.repository.StockLevelRepository;
import com.stockline.inventory.repository.StockLevelSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reading stock and adjusting it by hand. {@code available} is on hand minus reserved, always calculated.
 */
@Service
@Transactional(readOnly = true)
public class StockLevelService {

	private final StockLevelRepository stockLevelRepository;
	private final StockLevelMapper stockLevelMapper;
	private final ProductClient productClient;
	private final StockAdjuster stockAdjuster;

	public StockLevelService(StockLevelRepository stockLevelRepository, StockLevelMapper stockLevelMapper,
							 ProductClient productClient, StockAdjuster stockAdjuster) {
		this.stockLevelRepository = stockLevelRepository;
		this.stockLevelMapper = stockLevelMapper;
		this.productClient = productClient;
		this.stockAdjuster = stockAdjuster;
	}

	public PageResponse<StockLevelResponse> list(StockLevelFilter filter, Pageable pageable) {
		return PageResponse.from(stockLevelRepository.findAll(StockLevelSpecifications.matching(filter), pageable)
				.map(stockLevelMapper::toResponse));
	}

	public StockLevelResponse get(Long id) {
		return stockLevelRepository.findById(id)
				.map(stockLevelMapper::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException(StockLevel.class.getSimpleName(), id));
	}

	/**
	 * Changes on-hand stock by {@code quantity} and records a movement. The first adjustment of a product in a
	 * warehouse creates its stock level.
	 *
	 * <p>NOT_SUPPORTED: no transaction around the call to product-service, which would hold a database connection
	 * while waiting for the other service. The transaction is {@link StockAdjuster#apply}. The entity comes back
	 * detached there, but its warehouse was loaded in that transaction, so the response can be built here.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public StockLevelResponse adjust(AdjustStockRequest request) {
		if (request.quantity() == 0) {
			throw new InvalidRequestException(MessageKeys.STOCK_ADJUST_ZERO);
		}
		ProductSnapshot product = productClient.getProduct(request.productId());
		return stockLevelMapper.toResponse(stockAdjuster.apply(request, product));
	}
}
