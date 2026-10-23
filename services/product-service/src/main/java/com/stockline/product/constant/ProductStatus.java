package com.stockline.product.constant;

/**
 * Whether a product can be sold and ordered. Stored as a string in {@code products.status}.
 * "Low stock" is not a product status; inventory-service works that out from stock levels.
 */
public enum ProductStatus {
	ACTIVE,
	INACTIVE
}
