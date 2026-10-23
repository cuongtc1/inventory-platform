package com.stockline.product.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * One page of a list, in the JSON shape from docs/conventions.md section 4.
 * Used instead of Spring's {@link Page}, whose JSON contains many internal fields.
 */
public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages
) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}
}
