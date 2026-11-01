package com.marketplace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemAddRequest(
		@NotNull @Positive Long productId,
		@NotNull @Positive Integer quantity
) {
}
