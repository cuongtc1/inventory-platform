package com.stockline.product.dto;

import com.stockline.product.util.CommonValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body of POST and PUT /api/categories. {@code parentId} is null for a top-level category.
 */
public record CategoryRequest(
		@NotBlank @Size(max = CommonValidationUtils.NAME_MAX_LENGTH) String name,
		@Positive Long parentId
) {
}
