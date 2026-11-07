package com.stockline.order.mapper;

import com.stockline.order.dto.CustomerResponse;
import com.stockline.order.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

	public CustomerResponse toResponse(Customer customer, long orderCount) {
		return new CustomerResponse(
				customer.getId(),
				customer.getName(),
				customer.getEmail(),
				customer.getPhone(),
				orderCount,
				customer.getCreatedAt(),
				customer.getCreatedBy(),
				customer.getUpdatedAt(),
				customer.getUpdatedBy());
	}
}
