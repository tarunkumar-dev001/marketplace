package com.marketplace.dto;

import com.marketplace.entity.Cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartResponse(
		Long id,
		List<CartItemResponse> items,
		BigDecimal subtotal,
		Instant updatedAt
) {

	public static CartResponse from(Cart cart) {
		List<CartItemResponse> items = cart.getItems().stream().map(CartItemResponse::from).toList();
		BigDecimal subtotal = items.stream()
				.map(CartItemResponse::lineTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new CartResponse(cart.getId(), items, subtotal, cart.getUpdatedAt());
	}
}
