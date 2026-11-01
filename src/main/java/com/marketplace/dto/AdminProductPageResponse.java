package com.marketplace.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminProductPageResponse(
		List<AdminProductResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages
) {

	public static AdminProductPageResponse from(Page<AdminProductResponse> products) {
		return new AdminProductPageResponse(products.getContent(), products.getNumber(), products.getSize(),
				products.getTotalElements(), products.getTotalPages());
	}
}
