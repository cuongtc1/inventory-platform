package com.stockline.order.mapper;

import com.stockline.order.dto.SalesOrderResponse;
import com.stockline.order.dto.SalesOrderSummaryResponse;
import com.stockline.order.entity.SalesOrder;
import com.stockline.order.entity.SalesOrderItem;
import org.springframework.stereotype.Component;

@Component
public class SalesOrderMapper {

	/** Needs the customer and the lines loaded (SalesOrderRepository.findWithItemsById). */
	public SalesOrderResponse toResponse(SalesOrder order) {
		return new SalesOrderResponse(
				order.getId(),
				order.getOrderNumber(),
				order.getCustomer().getId(),
				order.getCustomer().getName(),
				order.getWarehouseId(),
				order.getWarehouseCode(),
				order.getStatus(),
				order.getTotalAmount(),
				order.getItems().stream().map(SalesOrderMapper::toItem).toList(),
				order.getCreatedAt(),
				order.getCreatedBy(),
				order.getUpdatedAt(),
				order.getUpdatedBy());
	}

	/** Needs the customer loaded (the list's entity graph); doesn't touch the lines. */
	public SalesOrderSummaryResponse toSummary(SalesOrder order) {
		return new SalesOrderSummaryResponse(
				order.getId(),
				order.getOrderNumber(),
				order.getCustomer().getId(),
				order.getCustomer().getName(),
				order.getWarehouseCode(),
				order.getStatus(),
				order.getTotalAmount(),
				order.getCreatedAt(),
				order.getCreatedBy());
	}

	private static SalesOrderResponse.Item toItem(SalesOrderItem item) {
		return new SalesOrderResponse.Item(
				item.getId(),
				item.getProductId(),
				item.getProductSku(),
				item.getProductName(),
				item.getQuantity(),
				item.getUnitPrice(),
				item.getLineTotal());
	}
}
