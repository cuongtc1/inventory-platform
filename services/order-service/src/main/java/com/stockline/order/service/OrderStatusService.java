package com.stockline.order.service;

import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.order.constant.OrderStatus;
import com.stockline.order.dto.OrderStatusHistoryResponse;
import com.stockline.order.dto.SalesOrderResponse;
import com.stockline.order.entity.OrderStatusHistory;
import com.stockline.order.entity.SalesOrder;
import com.stockline.order.mapper.SalesOrderMapper;
import com.stockline.order.repository.OrderStatusHistoryRepository;
import com.stockline.order.repository.SalesOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Every status change of a sales order goes through {@link #changeStatus}: the user's actions now (pay, ship,
 * complete, cancel), and the saga's RESERVED / REJECTED in Phase 6. One method, so the rule check
 * ({@link SalesOrder#changeStatus}) and the history row can't be skipped.
 */
@Service
@Transactional(readOnly = true)
public class OrderStatusService {

	private final SalesOrderRepository salesOrderRepository;
	private final OrderStatusHistoryRepository historyRepository;
	private final SalesOrderMapper salesOrderMapper;

	public OrderStatusService(SalesOrderRepository salesOrderRepository, OrderStatusHistoryRepository historyRepository,
							  SalesOrderMapper salesOrderMapper) {
		this.salesOrderRepository = salesOrderRepository;
		this.historyRepository = historyRepository;
		this.salesOrderMapper = salesOrderMapper;
	}

	/**
	 * Changes the status and records it, in one transaction.
	 *
	 * @param note why, when there is more to say (a rejected order names the product that was short); may be null
	 * @return the order after the change
	 * @throws com.stockline.common.exception.InvalidStatusTransitionException 409 when the current status doesn't
	 *                                                                         allow it; nothing is saved
	 */
	@Transactional
	public SalesOrderResponse changeStatus(Long orderId, OrderStatus next, String note) {
		SalesOrder order = findOrder(orderId);
		OrderStatus previous = order.changeStatus(next);
		historyRepository.save(new OrderStatusHistory(order, previous, next, note));
		// Run the UPDATE now: a version conflict (someone changed the order meanwhile) fails here with 409
		// CONCURRENT_UPDATE, and auditing fills updatedAt / updatedBy before the response is built
		salesOrderRepository.flush();
		return salesOrderMapper.toResponse(order);
	}

	/** The timeline of an order, oldest first. */
	public List<OrderStatusHistoryResponse> history(Long orderId) {
		if (!salesOrderRepository.existsById(orderId)) {
			throw notFound(orderId);
		}
		return historyRepository.findBySalesOrderIdOrderByCreatedAtAscIdAsc(orderId).stream()
				.map(row -> new OrderStatusHistoryResponse(row.getId(), row.getFromStatus(), row.getToStatus(),
						row.getNote(), row.getCreatedAt(), row.getCreatedBy()))
				.toList();
	}

	/** With customer and lines, so the response can be built from it. */
	private SalesOrder findOrder(Long id) {
		return salesOrderRepository.findWithItemsById(id).orElseThrow(() -> notFound(id));
	}

	private static ResourceNotFoundException notFound(Long id) {
		return new ResourceNotFoundException(SalesOrder.class.getSimpleName(), id);
	}
}
