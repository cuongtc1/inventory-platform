package com.stockline.order.entity;

import com.stockline.common.entity.BaseEntity;
import com.stockline.common.util.MoneyUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * One line of a sales order. SKU, name and price are copies from product-service at the moment of the order; nothing
 * changes them afterwards. Created through {@link SalesOrder#addItem}.
 */
@Entity
@Table(name = "sales_order_items")
public class SalesOrderItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sales_order_id")
	private SalesOrder salesOrder;

	// product-service's product: an id and copies, no relationship
	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "product_sku", nullable = false, length = 50)
	private String productSku;

	@Column(name = "product_name", nullable = false, length = 200)
	private String productName;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	protected SalesOrderItem() {
		// for JPA
	}

	SalesOrderItem(SalesOrder salesOrder, Long productId, String productSku, String productName, int quantity,
				   BigDecimal unitPrice) {
		this.salesOrder = salesOrder;
		this.productId = productId;
		this.productSku = productSku;
		this.productName = productName;
		this.quantity = quantity;
		this.unitPrice = MoneyUtils.toMoney(unitPrice);
	}

	/** Quantity × unit price, calculated and never stored (docs/conventions.md: money). */
	public BigDecimal getLineTotal() {
		return MoneyUtils.multiply(unitPrice, quantity);
	}

	public SalesOrder getSalesOrder() {
		return salesOrder;
	}

	public Long getProductId() {
		return productId;
	}

	public String getProductSku() {
		return productSku;
	}

	public String getProductName() {
		return productName;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}
}
