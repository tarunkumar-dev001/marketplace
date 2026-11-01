package com.marketplace.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record OrderPageResponse(
		List<OrderResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages
) {

	public static OrderPageResponse from(Page<OrderResponse> orders) {
		return new OrderPageResponse(
				orders.getContent(),
				orders.getNumber(),
				orders.getSize(),
				orders.getTotalElements(),
				orders.getTotalPages()
		);
	}
}
