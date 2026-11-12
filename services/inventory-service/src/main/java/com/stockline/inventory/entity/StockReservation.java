package com.stockline.inventory.entity;

import com.stockline.common.entity.BaseEntity;
import com.stockline.inventory.constant.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Stock set aside for one line of a sales order. {@code orderId} points to order-service's data, so it is a plain
 * column. The table allows one row per order, warehouse and product, so the same order can't reserve a line twice.
 */
@Entity
@Table(name = "stock_reservations")
public class StockReservation extends BaseEntity {

	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(nullable = false)
	private int quantity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReservationStatus status;

	protected StockReservation() {
		// for JPA
	}

	/** A new reservation, in RESERVED. */
	public StockReservation(Long orderId, StockLevel level, int quantity) {
		this.orderId = orderId;
		this.warehouse = level.getWarehouse();
		this.productId = level.getProductId();
		this.quantity = quantity;
		this.status = ReservationStatus.RESERVED;
	}

	public Long getOrderId() {
		return orderId;
	}

	public Warehouse getWarehouse() {
		return warehouse;
	}

	public Long getProductId() {
		return productId;
	}

	public int getQuantity() {
		return quantity;
	}

	public ReservationStatus getStatus() {
		return status;
	}
}
