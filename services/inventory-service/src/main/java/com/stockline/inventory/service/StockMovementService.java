package com.stockline.inventory.service;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.InvalidRequestException;
import com.stockline.inventory.constant.MessageKeys;
import com.stockline.inventory.dto.StockMovementFilter;
import com.stockline.inventory.dto.StockMovementResponse;
import com.stockline.inventory.mapper.StockMovementMapper;
import com.stockline.inventory.repository.StockMovementRepository;
import com.stockline.inventory.repository.StockMovementSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reading the history of stock. Read-only on purpose: movements are written by the code that changes stock, in the
 * same transaction, and never changed afterwards.
 */
@Service
@Transactional(readOnly = true)
public class StockMovementService {

	private final StockMovementRepository stockMovementRepository;
	private final StockMovementMapper stockMovementMapper;

	public StockMovementService(StockMovementRepository stockMovementRepository,
								StockMovementMapper stockMovementMapper) {
		this.stockMovementRepository = stockMovementRepository;
		this.stockMovementMapper = stockMovementMapper;
	}

	public PageResponse<StockMovementResponse> list(StockMovementFilter filter, Pageable pageable) {
		if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
			throw new InvalidRequestException(MessageKeys.STOCK_MOVEMENT_DATE_RANGE);
		}
		return PageResponse.from(stockMovementRepository
				.findAll(StockMovementSpecifications.matching(filter), pageable)
				.map(stockMovementMapper::toResponse));
	}
}
