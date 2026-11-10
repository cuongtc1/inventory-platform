package com.stockline.order.entity;

import com.stockline.common.entity.BaseEntity;
import com.stockline.order.constant.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One status change of an order, for its timeline. Never changed after it is saved; {@code createdAt} and
 * {@code createdBy} say when and who.
 */
@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sales_order_id")
	private SalesOrder salesOrder;

	/** Null on the first row: the order had no status before it was created. */
	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 20)
	private OrderStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false, length = 20)
	private OrderStatus toStatus;

	@Column(length = 255)
	private String note;

	protected OrderStatusHistory() {
		// for JPA
	}

	public OrderStatusHistory(SalesOrder salesOrder, OrderStatus fromStatus, OrderStatus toStatus, String note) {
		this.salesOrder = salesOrder;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.note = note;
	}

	public SalesOrder getSalesOrder() {
		return salesOrder;
	}

	public OrderStatus getFromStatus() {
		return fromStatus;
	}

	public OrderStatus getToStatus() {
		return toStatus;
	}

	public String getNote() {
		return note;
	}
}
