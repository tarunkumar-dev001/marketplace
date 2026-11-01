package com.marketplace.dto;

import com.marketplace.entity.CustomerOrder;
import com.marketplace.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
		Long orderId,
		OrderStatus status,
		String currency,
		BigDecimal totalAmount,
		Instant createdAt,
		Instant updatedAt,
		List<OrderItemResponse> items
) {

	public static OrderResponse from(CustomerOrder order) {
		return new OrderResponse(
				order.getId(),
				order.getStatus(),
				order.getCurrency(),
				order.getTotalAmount(),
				order.getCreatedAt(),
				order.getUpdatedAt(),
				order.getItems().stream().map(OrderItemResponse::from).toList()
		);
	}

	public static OrderResponse from(CustomerOrder order, Long vendorId) {
		List<com.marketplace.entity.OrderItem> vendorItems = order.getItems().stream()
				.filter(item -> vendorId.equals(item.getVendor() == null ? null : item.getVendor().getId()))
				.toList();
		BigDecimal vendorSubtotal = vendorItems.stream()
				.map(com.marketplace.entity.OrderItem::getLineTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new OrderResponse(
				order.getId(),
				order.getStatus(),
				order.getCurrency(),
				vendorSubtotal,
				order.getCreatedAt(),
				order.getUpdatedAt(),
				vendorItems.stream()
						.map(OrderItemResponse::from)
						.toList()
		);
	}
}
