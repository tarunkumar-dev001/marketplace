package com.marketplace.dto;

import com.marketplace.entity.CustomerOrder;
import com.marketplace.enums.OrderStatus;
import com.marketplace.enums.PaymentMethod;
import com.marketplace.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CheckoutResponse(
		Long orderId,
		OrderStatus status,
		String currency,
		BigDecimal totalAmount,
		Instant createdAt,
		String shippingName, String shippingPhone, String shippingAddress, String shippingCity, String shippingPincode,
		PaymentMethod paymentMethod, PaymentStatus paymentStatus,
		List<CheckoutItemResponse> items
) {

	public static CheckoutResponse from(CustomerOrder order) {
		return new CheckoutResponse(
				order.getId(),
				order.getStatus(),
				order.getCurrency(),
				order.getTotalAmount(),
				order.getCreatedAt(),
				order.getShippingName(), order.getShippingPhone(), order.getShippingAddress(),
				order.getShippingCity(), order.getShippingPincode(), order.getPaymentMethod(), order.getPaymentStatus(),
				order.getItems().stream().map(CheckoutItemResponse::from).toList()
		);
	}
}
