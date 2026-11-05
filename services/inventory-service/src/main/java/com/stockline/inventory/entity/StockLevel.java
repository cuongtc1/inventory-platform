package com.stockline.inventory.entity;

import com.stockline.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * How much of one product one warehouse holds. {@code productId} points to product-service's data, so it is a plain
 * value with copies of what lists need, not a JPA relation.
 */
@Entity
@Table(name = "stock_levels")
public class StockLevel extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "product_sku", nullable = false, length = 50)
	private String productSku;

	@Column(name = "product_name", nullable = false, length = 200)
	private String productName;

	@Column(name = "reorder_level", nullable = false)
	private int reorderLevel;

	@Column(name = "on_hand", nullable = false)
	private int onHand;

	@Column(nullable = false)
	private int reserved;

	/**
	 * Optimistic locking: Hibernate adds {@code WHERE version = ?} to each UPDATE and raises the number. If no row
	 * matches, someone else changed it first, and the save fails instead of overwriting their change.
	 */
	@Version
	@Column(nullable = false)
	private Long version;

	protected StockLevel() {
		// for JPA
	}

	/** A product new to this warehouse, with nothing in stock yet. */
	public StockLevel(Warehouse warehouse, Long productId) {
		this.warehouse = warehouse;
		this.productId = productId;
	}

	/** Calculated, never stored, so it can't disagree with the two numbers it comes from. */
	public int getAvailable() {
		return onHand - reserved;
	}

	/** At or below the reorder level. The same rule as {@code StockLevelSpecifications.lowStock()}. */
	public boolean isLowStock() {
		return getAvailable() <= reorderLevel;
	}

	/** Refreshes the copies from product-service. */
	public void copyProduct(String sku, String name, int reorderLevel) {
		this.productSku = sku;
		this.productName = name;
		this.reorderLevel = reorderLevel;
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

	public int getReorderLevel() {
		return reorderLevel;
	}

	public int getOnHand() {
		return onHand;
	}

	public void setOnHand(int onHand) {
		this.onHand = onHand;
	}

	public int getReserved() {
		return reserved;
	}

	public void setReserved(int reserved) {
		this.reserved = reserved;
	}

	public Long getVersion() {
		return version;
	}
}
