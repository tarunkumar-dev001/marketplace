package com.marketplace.dto;

import com.marketplace.entity.CustomerOrder;
import com.marketplace.entity.OrderItem;
import com.marketplace.entity.User;
import com.marketplace.enums.OrderStatus;
import com.marketplace.enums.Role;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderResponseTests {

	@Test
	void vendorProjectionUsesOnlyVendorOwnedLineTotals() {
		CustomerOrder order = mock(CustomerOrder.class);
		User vendor = mock(User.class);
		User otherVendor = mock(User.class);
		when(vendor.getId()).thenReturn(8L);
		when(otherVendor.getId()).thenReturn(9L);
		OrderItem vendorItem = mock(OrderItem.class);
		OrderItem otherItem = mock(OrderItem.class);
		when(vendorItem.getVendor()).thenReturn(vendor);
		when(vendorItem.getLineTotal()).thenReturn(new BigDecimal("25.50"));
		when(otherItem.getVendor()).thenReturn(otherVendor);
		when(otherItem.getLineTotal()).thenReturn(new BigDecimal("74.50"));
		when(vendorItem.getProduct()).thenReturn(mock(com.marketplace.entity.Product.class));
		when(vendorItem.getProduct().getId()).thenReturn(101L);
		when(vendorItem.getProductName()).thenReturn("Vendor product");
		when(vendorItem.getUnitPrice()).thenReturn(new BigDecimal("25.50"));
		when(vendorItem.getQuantity()).thenReturn(1);
		when(order.getItems()).thenReturn(List.of(vendorItem, otherItem));
		when(order.getTotalAmount()).thenReturn(new BigDecimal("100.00"));
		when(order.getStatus()).thenReturn(OrderStatus.PLACED);
		when(order.getCurrency()).thenReturn("USD");

		OrderResponse response = OrderResponse.from(order, 8L);

		assertEquals(new BigDecimal("25.50"), response.totalAmount());
		assertEquals(1, response.items().size());
	}
}
