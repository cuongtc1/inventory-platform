package com.stockline.product.dto;

import com.stockline.product.entity.ProductStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body of POST and PUT /api/products. Only {@code supplierId} is optional.
 */
public record ProductRequest(
		@NotBlank @Size(max = 50)
		@Pattern(regexp = "[A-Za-z0-9._-]*", message = "{product.sku.invalid}")
		String sku,
		@NotBlank @Size(max = 200) String name,
		@NotNull @Positive Long categoryId,
		@Positive Long supplierId,
		// Digits matches the DECIMAL(12,2) column: at most 10 digits before the point and 2 after
		@NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal costPrice,
		@NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal salePrice,
		@NotNull @PositiveOrZero Integer reorderLevel,
		@NotNull ProductStatus status
) {
}
