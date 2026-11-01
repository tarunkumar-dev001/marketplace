package com.marketplace.dto;

import com.marketplace.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
		Long productId,
		String productName,
		BigDecimal unitPrice,
		Integer quantity,
		BigDecimal lineTotal
) {

	public static OrderItemResponse from(OrderItem item) {
		return new OrderItemResponse(
				item.getProduct().getId(),
				item.getProductName(),
				item.getUnitPrice(),
				item.getQuantity(),
				item.getLineTotal()
		);
	}
}
