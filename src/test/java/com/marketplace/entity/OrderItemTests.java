package com.marketplace.entity;

import com.marketplace.enums.OrderStatus;
import com.marketplace.enums.Role;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderItemTests {

	@Test
	void constructorPreservesPurchaseTimeSnapshots() {
		User customer = new User("customer@example.com", "password-hash", Role.CUSTOMER, true);
		User vendor = new User("vendor@example.com", "password-hash", Role.VENDOR, true);
		CustomerOrder order = new CustomerOrder(customer, OrderStatus.PLACED, "INR", new BigDecimal("20.00"));
		Product product = new Product("Current Product", "Description", new BigDecimal("12.00"), 3, "Books", true, vendor);

		OrderItem item = new OrderItem(order, product, vendor, "Purchased Product", new BigDecimal("10.00"), 2,
				new BigDecimal("20.00"));

		assertEquals(order, item.getOrder());
		assertEquals(product, item.getProduct());
		assertEquals(vendor, item.getVendor());
		assertEquals("Purchased Product", item.getProductName());
		assertEquals(new BigDecimal("10.00"), item.getUnitPrice());
		assertEquals(2, item.getQuantity());
		assertEquals(new BigDecimal("20.00"), item.getLineTotal());
	}
}
