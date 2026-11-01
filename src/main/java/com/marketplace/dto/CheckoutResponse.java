package com.marketplace.dto;

import com.marketplace.entity.CustomerOrder;
import com.marketplace.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CheckoutResponse(
		Long orderId,
		OrderStatus status,
		String currency,
		BigDecimal totalAmount,
		Instant createdAt,
		List<CheckoutItemResponse> items
) {

	public static CheckoutResponse from(CustomerOrder order) {
		return new CheckoutResponse(
				order.getId(),
				order.getStatus(),
				order.getCurrency(),
				order.getTotalAmount(),
				order.getCreatedAt(),
				order.getItems().stream().map(CheckoutItemResponse::from).toList()
		);
	}
}
