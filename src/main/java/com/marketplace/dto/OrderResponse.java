package com.marketplace.dto;

import com.marketplace.entity.CustomerOrder;
import com.marketplace.enums.OrderStatus;
import com.marketplace.enums.PaymentMethod;
import com.marketplace.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
		Long orderId,
		OrderStatus status,
		String customerEmail,
		String currency,
		BigDecimal totalAmount,
		Instant createdAt,
		Instant updatedAt,
		String shippingName, String shippingPhone, String shippingAddress, String shippingCity, String shippingPincode,
		PaymentMethod paymentMethod, PaymentStatus paymentStatus,
		List<OrderItemResponse> items
) {

	public static OrderResponse from(CustomerOrder order) {
		return new OrderResponse(
				order.getId(),
				order.getStatus(),
				null,
				order.getCurrency(),
				order.getTotalAmount(),
				order.getCreatedAt(),
				order.getUpdatedAt(),
				order.getShippingName(), order.getShippingPhone(), order.getShippingAddress(),
				order.getShippingCity(), order.getShippingPincode(), order.getPaymentMethod(), order.getPaymentStatus(),
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
				null,
				order.getCurrency(),
				vendorSubtotal,
				order.getCreatedAt(),
				order.getUpdatedAt(),
				order.getShippingName(), order.getShippingPhone(), order.getShippingAddress(),
				order.getShippingCity(), order.getShippingPincode(), order.getPaymentMethod(), order.getPaymentStatus(),
				vendorItems.stream()
						.map(OrderItemResponse::from)
						.toList()
		);
	}

	public static OrderResponse adminFrom(CustomerOrder order) {
		return new OrderResponse(
				order.getId(),
				order.getStatus(),
				order.getCustomer() == null ? null : order.getCustomer().getEmail(),
				order.getCurrency(),
				order.getTotalAmount(),
				order.getCreatedAt(),
				order.getUpdatedAt(),
				order.getShippingName(), order.getShippingPhone(), order.getShippingAddress(),
				order.getShippingCity(), order.getShippingPincode(), order.getPaymentMethod(), order.getPaymentStatus(),
				order.getItems().stream().map(OrderItemResponse::from).toList()
		);
	}
}
