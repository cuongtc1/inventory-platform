package com.stockline.order.service;

import com.stockline.common.dto.PageResponse;
import com.stockline.common.exception.InvalidRequestException;
import com.stockline.common.exception.ResourceNotFoundException;
import com.stockline.common.util.MoneyUtils;
import com.stockline.order.client.ProductClient;
import com.stockline.order.client.WarehouseClient;
import com.stockline.order.constant.MessageKeys;
import com.stockline.order.dto.CreateSalesOrderRequest;
import com.stockline.order.dto.ProductSnapshot;
import com.stockline.order.dto.SalesOrderFilter;
import com.stockline.order.dto.SalesOrderResponse;
import com.stockline.order.dto.SalesOrderSummaryResponse;
import com.stockline.order.dto.WarehouseSnapshot;
import com.stockline.order.entity.Customer;
import com.stockline.order.entity.SalesOrder;
import com.stockline.order.mapper.SalesOrderMapper;
import com.stockline.order.repository.CustomerRepository;
import com.stockline.order.repository.SalesOrderRepository;
import com.stockline.order.repository.SalesOrderSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sales orders: listing, reading, and creating them as PENDING. Status changes come with T5.4.
 */
@Service
@Transactional(readOnly = true)
public class SalesOrderService {

	private final SalesOrderRepository salesOrderRepository;
	private final CustomerRepository customerRepository;
	private final SalesOrderMapper salesOrderMapper;
	private final ProductClient productClient;
	private final WarehouseClient warehouseClient;
	private final SalesOrderCreator salesOrderCreator;

	public SalesOrderService(SalesOrderRepository salesOrderRepository, CustomerRepository customerRepository,
							 SalesOrderMapper salesOrderMapper, ProductClient productClient,
							 WarehouseClient warehouseClient, SalesOrderCreator salesOrderCreator) {
		this.salesOrderRepository = salesOrderRepository;
		this.customerRepository = customerRepository;
		this.salesOrderMapper = salesOrderMapper;
		this.productClient = productClient;
		this.warehouseClient = warehouseClient;
		this.salesOrderCreator = salesOrderCreator;
	}

	public PageResponse<SalesOrderSummaryResponse> list(SalesOrderFilter filter, Pageable pageable) {
		return PageResponse.from(salesOrderRepository.findAll(SalesOrderSpecifications.matching(filter), pageable)
				.map(salesOrderMapper::toSummary));
	}

	public SalesOrderResponse get(Long id) {
		return salesOrderRepository.findWithItemsById(id)
				.map(salesOrderMapper::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException(SalesOrder.class.getSimpleName(), id));
	}

	/**
	 * Checks the request, reads the warehouse and every product from their services, then saves the order in
	 * {@link SalesOrderCreator}. Cheap checks first, so a bad request costs no calls to other services.
	 *
	 * <p>NOT_SUPPORTED: no transaction around the calls to other services, which would hold a database connection
	 * while waiting for them.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public SalesOrderResponse create(CreateSalesOrderRequest request) {
		checkNoProductTwice(request);
		if (!customerRepository.existsById(request.customerId())) {
			throw new ResourceNotFoundException(Customer.class.getSimpleName(), request.customerId());
		}

		WarehouseSnapshot warehouse = warehouseClient.getWarehouse(request.warehouseId());
		Map<Long, ProductSnapshot> products = new LinkedHashMap<>();
		for (CreateSalesOrderRequest.Item item : request.items()) {
			ProductSnapshot product = productClient.getProduct(item.productId());
			if (!product.isActive()) {
				throw new InvalidRequestException(MessageKeys.ORDER_PRODUCT_INACTIVE, product.sku());
			}
			products.put(product.id(), product);
		}
		checkTotalFits(request, products);

		// The order and its order.created event are saved together (outbox); inventory-service then reserves the stock
		return salesOrderMapper.toResponse(salesOrderCreator.create(request, warehouse, products));
	}

	/** One line per product, like the table's unique key and a stock reservation; the client adds up quantities. */
	private static void checkNoProductTwice(CreateSalesOrderRequest request) {
		Set<Long> seen = new HashSet<>();
		for (CreateSalesOrderRequest.Item item : request.items()) {
			if (!seen.add(item.productId())) {
				throw new InvalidRequestException(MessageKeys.ORDER_PRODUCT_TWICE, item.productId());
			}
		}
	}

	/** Before the transaction: a total DECIMAL(12,2) can't hold would otherwise fail as a database error. */
	private static void checkTotalFits(CreateSalesOrderRequest request, Map<Long, ProductSnapshot> products) {
		BigDecimal total = MoneyUtils.sum(request.items().stream()
				.map(item -> MoneyUtils.multiply(products.get(item.productId()).salePrice(), item.quantity()))
				.toList());
		if (total.compareTo(SalesOrder.MAX_TOTAL) > 0) {
			throw new InvalidRequestException(MessageKeys.ORDER_TOTAL_TOO_LARGE, MoneyUtils.format(total),
					MoneyUtils.format(SalesOrder.MAX_TOTAL));
		}
	}
}
