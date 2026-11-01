package com.marketplace.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record ProductPageResponse(
		List<ProductResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages
) {

	public static ProductPageResponse from(Page<ProductResponse> products) {
		return new ProductPageResponse(
				products.getContent(),
				products.getNumber(),
				products.getSize(),
				products.getTotalElements(),
				products.getTotalPages()
		);
	}
}
