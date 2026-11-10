package com.stockline.order.entity;

import com.stockline.common.entity.BaseEntity;
import com.stockline.common.exception.InvalidStatusTransitionException;
import com.stockline.common.util.MoneyUtils;
import com.stockline.order.constant.MessageKeys;
import com.stockline.order.constant.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A sales order with its lines. The lines are added when the order is created and never change afterwards, so
 * {@code totalAmount} (stored for the list) can't disagree with them. There is no setter for the status: it changes only
 * through {@link #changeStatus}, which asks {@link OrderStatus#canChangeTo} first.
 */
@Entity
@Table(name = "sales_orders")
public class SalesOrder extends BaseEntity {

	/** The largest amount DECIMAL(12,2) holds: 10 digits before the point, 2 after. */
	public static final BigDecimal MAX_TOTAL = new BigDecimal("9999999999.99");

	@Column(name = "order_number", nullable = false, length = 20)
	private String orderNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id")
	private Customer customer;

	// inventory-service's warehouse: an id and a copy of its code, no relationship
	@Column(name = "warehouse_id", nullable = false)
	private Long warehouseId;

	@Column(name = "warehouse_code", nullable = false, length = 50)
	private String warehouseCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Version
	private long version;

	// cascade: saving the order saves its lines. mappedBy: the foreign key is on the item's side
	@OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL)
	@OrderBy("id")
	private List<SalesOrderItem> items = new ArrayList<>();

	protected SalesOrder() {
		// for JPA
	}

	/** A new order starts as PENDING with no lines and a total of 0.00. */
	public SalesOrder(String orderNumber, Customer customer, Long warehouseId, String warehouseCode) {
		this.orderNumber = orderNumber;
		this.customer = customer;
		this.warehouseId = warehouseId;
		this.warehouseCode = warehouseCode;
		this.status = OrderStatus.PENDING;
		this.totalAmount = MoneyUtils.ZERO;
	}

	/** Adds a line with the product's data of this moment, and updates the total. */
	public void addItem(Long productId, String productSku, String productName, int quantity, BigDecimal unitPrice) {
		items.add(new SalesOrderItem(this, productId, productSku, productName, quantity, unitPrice));
		totalAmount = MoneyUtils.sum(items.stream().map(SalesOrderItem::getLineTotal).toList());
	}

	/**
	 * Moves the order to {@code next} if its current status allows it; otherwise nothing changes.
	 *
	 * @return the status before the change, for the history
	 * @throws InvalidStatusTransitionException 409, e.g. shipping an order that isn't paid
	 */
	public OrderStatus changeStatus(OrderStatus next) {
		if (!status.canChangeTo(next)) {
			throw new InvalidStatusTransitionException(MessageKeys.ORDER_STATUS_CHANGE_NOT_ALLOWED, orderNumber, status,
					next);
		}
		OrderStatus previous = status;
		status = next;
		return previous;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public Customer getCustomer() {
		return customer;
	}

	public Long getWarehouseId() {
		return warehouseId;
	}

	public String getWarehouseCode() {
		return warehouseCode;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public long getVersion() {
		return version;
	}

	/** Read-only: lines are added with {@link #addItem} only. */
	public List<SalesOrderItem> getItems() {
		return Collections.unmodifiableList(items);
	}
}
