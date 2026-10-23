package com.stockline.product.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST and PUT /api/suppliers. Only {@code name} is required.
 */
public record SupplierRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 100) String contactName,
        // @Email accepts null and "", so an empty field passes; the service stores it as null
        @Email @Size(max = 150) String email,
        @Pattern(regexp = "[0-9+()\\- ]*", message = "may contain only digits, spaces, +, -, ( and )")
        @Size(max = 30) String phone
) {
}
