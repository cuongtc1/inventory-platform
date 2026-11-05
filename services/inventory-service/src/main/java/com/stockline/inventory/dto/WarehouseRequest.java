package com.stockline.inventory.dto;

import com.stockline.common.util.CommonValidationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST and PUT /api/warehouses. Only {@code address} is optional.
 */
public record WarehouseRequest(
		@NotBlank @Size(max = CommonValidationUtils.CODE_MAX_LENGTH)
		@Pattern(regexp = CommonValidationUtils.CODE_REGEX, message = CommonValidationUtils.CODE_MESSAGE)
		String code,
		@NotBlank @Size(max = CommonValidationUtils.NAME_MAX_LENGTH) String name,
		@Size(max = 255) String address
) {
}
