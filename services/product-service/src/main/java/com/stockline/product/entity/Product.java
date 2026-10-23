package com.stockline.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * A product in the catalog (table {@code products}). The SKU is unique.
 */
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

	// Unique in the database (uk_products_sku in V3__create_products.sql)
	@Column(nullable = false, length = 50)
	private String sku;

	@Column(nullable = false, length = 200)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	// Optional: a product can exist before we pick a supplier
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id")
	private Supplier supplier;

	@Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal costPrice;

	@Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal salePrice;

	@Column(name = "reorder_level", nullable = false)
	private int reorderLevel;

	// STRING stores "ACTIVE", not the enum's position (0), so reordering the enum can't corrupt data
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProductStatus status;

	protected Product() {
		// for JPA
	}

	public Product(String sku, String name, Category category, Supplier supplier,
				   BigDecimal costPrice, BigDecimal salePrice, int reorderLevel, ProductStatus status) {
		this.sku = sku;
		this.name = name;
		this.category = category;
		this.supplier = supplier;
		this.costPrice = costPrice;
		this.salePrice = salePrice;
		this.reorderLevel = reorderLevel;
		this.status = status;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public Supplier getSupplier() {
		return supplier;
	}

	public void setSupplier(Supplier supplier) {
		this.supplier = supplier;
	}

	public BigDecimal getCostPrice() {
		return costPrice;
	}

	public void setCostPrice(BigDecimal costPrice) {
		this.costPrice = costPrice;
	}

	public BigDecimal getSalePrice() {
		return salePrice;
	}

	public void setSalePrice(BigDecimal salePrice) {
		this.salePrice = salePrice;
	}

	public int getReorderLevel() {
		return reorderLevel;
	}

	public void setReorderLevel(int reorderLevel) {
		this.reorderLevel = reorderLevel;
	}

	public ProductStatus getStatus() {
		return status;
	}

	public void setStatus(ProductStatus status) {
		this.status = status;
	}
}
