package com.stockline.order.service;

import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.order.constant.OrderStatus;
import com.stockline.order.dto.CreateSalesOrderRequest;
import com.stockline.order.dto.ProductSnapshot;
import com.stockline.order.dto.WarehouseSnapshot;
import com.stockline.order.entity.Customer;
import com.stockline.order.entity.OrderStatusHistory;
import com.stockline.order.entity.SalesOrder;
import com.stockline.order.event.OrderEvents;
import com.stockline.order.repository.CustomerRepository;
import com.stockline.order.repository.OrderStatusHistoryRepository;
import com.stockline.order.repository.SalesOrderRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * The database part of creating an order, in one transaction: number, order, lines, the first history row and the
 * order.created event (outbox) are saved together or not at all. A separate bean from {@link SalesOrderService},
 * because {@code @Transactional} only works on calls from another bean, and the calls to the other services must stay
 * outside the transaction (the same split as inventory-service's StockAdjuster).
 */
@Component
public class SalesOrderCreator {

	private final CustomerRepository customerRepository;
	private final SalesOrderRepository salesOrderRepository;
	private final OrderStatusHistoryRepository historyRepository;
	private final OrderNumberGenerator orderNumberGenerator;
	private final OrderEvents orderEvents;

	public SalesOrderCreator(CustomerRepository customerRepository, SalesOrderRepository salesOrderRepository,
							 OrderStatusHistoryRepository historyRepository, OrderNumberGenerator orderNumberGenerator,
							 OrderEvents orderEvents) {
		this.customerRepository = customerRepository;
		this.salesOrderRepository = salesOrderRepository;
		this.historyRepository = historyRepository;
		this.orderNumberGenerator = orderNumberGenerator;
		this.orderEvents = orderEvents;
	}

	/**
	 * @param products every product of the request, by id, already checked by the caller
	 * @return the saved order with its customer and lines loaded, so the response can be built after the transaction
	 */
	@Transactional
	public SalesOrder create(CreateSalesOrderRequest request, WarehouseSnapshot warehouse,
							 Map<Long, ProductSnapshot> products) {
		// Checked before the calls to the other services too; looked up again because it could be gone since
		Customer customer = customerRepository.findById(request.customerId())
				.orElseThrow(() -> new ResourceNotFoundException(Customer.class.getSimpleName(), request.customerId()));

		// Last step before saving: the number's row stays locked until the commit, so keep the rest short
		SalesOrder order = new SalesOrder(orderNumberGenerator.nextSalesOrderNumber(), customer, warehouse.id(),
				warehouse.code());
		for (CreateSalesOrderRequest.Item item : request.items()) {
			ProductSnapshot product = products.get(item.productId());
			order.addItem(product.id(), product.sku(), product.name(), item.quantity(), product.salePrice());
		}

		SalesOrder saved = salesOrderRepository.save(order);
		historyRepository.save(new OrderStatusHistory(saved, null, OrderStatus.PENDING, null));
		// order.created into the outbox, in this transaction: saved with the order or not at all (T6.5)
		orderEvents.orderCreated(saved);
		// Run the SQL now, so auditing fills createdAt / createdBy before the response is built
		salesOrderRepository.flush();
		return saved;
	}
}
