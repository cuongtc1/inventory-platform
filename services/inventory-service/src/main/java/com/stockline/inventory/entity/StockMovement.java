package com.stockline.inventory.entity;

import com.stockline.common.entity.BaseEntity;
import com.stockline.inventory.constant.MovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One change of stock, for the history: when and who are the audit columns. Never changed after it is written, so
 * there are no setters. {@code quantity} is signed: an ADJUST of -2 took two away.
 */
@Entity
@Table(name = "stock_movements")
public class StockMovement extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "product_sku", nullable = false, length = 50)
	private String productSku;

	@Column(name = "product_name", nullable = false, length = 200)
	private String productName;

	@Enumerated(EnumType.STRING)
	@Column(name = "movement_type", nullable = false, length = 20)
	private MovementType movementType;

	@Column(nullable = false)
	private int quantity;

	@Column(length = 100)
	private String reference;

	protected StockMovement() {
		// for JPA
	}

	public StockMovement(StockLevel level, MovementType movementType, int quantity, String reference) {
		this.warehouse = level.getWarehouse();
		this.productId = level.getProductId();
		this.productSku = level.getProductSku();
		this.productName = level.getProductName();
		this.movementType = movementType;
		this.quantity = quantity;
		this.reference = reference;
	}

	public Warehouse getWarehouse() {
		return warehouse;
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

	public MovementType getMovementType() {
		return movementType;
	}

	public int getQuantity() {
		return quantity;
	}

	public String getReference() {
		return reference;
	}
}
