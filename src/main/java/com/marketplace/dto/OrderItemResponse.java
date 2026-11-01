package com.marketplace.dto;

import com.marketplace.entity.OrderItem;
import com.marketplace.enums.FulfillmentStatus;

import java.math.BigDecimal;

public record OrderItemResponse(
		Long itemId,
		Long productId,
		String productName,
		BigDecimal unitPrice,
		Integer quantity,
		BigDecimal lineTotal,
		FulfillmentStatus fulfillmentStatus
) {

	public static OrderItemResponse from(OrderItem item) {
		return new OrderItemResponse(
				item.getId(),
				item.getProduct().getId(),
				item.getProductName(),
				item.getUnitPrice(),
				item.getQuantity(),
				item.getLineTotal(),
				item.getFulfillmentStatus()
		);
	}
}
