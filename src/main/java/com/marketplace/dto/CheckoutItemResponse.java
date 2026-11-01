package com.marketplace.dto;

import com.marketplace.entity.OrderItem;

import java.math.BigDecimal;

public record CheckoutItemResponse(
		Long productId,
		String productName,
		BigDecimal unitPrice,
		Integer quantity,
		BigDecimal lineTotal
) {

	public static CheckoutItemResponse from(OrderItem item) {
		return new CheckoutItemResponse(
				item.getProduct().getId(),
				item.getProductName(),
				item.getUnitPrice(),
				item.getQuantity(),
				item.getLineTotal()
		);
	}
}
