package com.marketplace.dto;

import com.marketplace.entity.CartItem;

import java.math.BigDecimal;

public record CartItemResponse(
		Long id,
		Long productId,
		String productName,
		BigDecimal price,
		Integer quantity,
		BigDecimal lineTotal
) {

	public static CartItemResponse from(CartItem item) {
		BigDecimal lineTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
		return new CartItemResponse(
				item.getId(),
				item.getProduct().getId(),
				item.getProduct().getName(),
				item.getProduct().getPrice(),
				item.getQuantity(),
				lineTotal
		);
	}
}
