package com.marketplace.service.impl;

import com.marketplace.entity.CustomerOrder;
import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.enums.OrderStatus;
import com.marketplace.repository.CustomerOrderRepository;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketplaceOrderStatisticsTests {

	private CustomerOrderRepository orders;
	private UserRepository users;
	private OrderService service;

	@BeforeEach
	void setUp() {
		orders = mock(CustomerOrderRepository.class);
		users = mock(UserRepository.class);
		service = new OrderServiceImpl(orders, mock(ProductRepository.class), users);
	}

	@Test
	void vendorPageUsesDistinctVendorOrderQueryAndStatisticsUseVendorFixtures() {
		User vendor = new User("vendor@example.com", "hash", Role.VENDOR, true);
		setId(vendor, 8L);
		CustomerOrder first = mock(CustomerOrder.class);
		CustomerOrder second = mock(CustomerOrder.class);
		when(first.getId()).thenReturn(11L);
		when(second.getId()).thenReturn(10L);
		var pageable = PageRequest.of(0, 20);
		when(users.findById(8L)).thenReturn(Optional.of(vendor));
		when(orders.findDistinctByVendorId(8L, pageable))
				.thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));
		when(orders.findByVendorIdAndIdInWithItems(8L, List.of(11L, 10L)))
				.thenReturn(List.of(second, first));
		when(orders.countVendorOrders(8L)).thenReturn(2L);
		when(orders.countVendorOrdersByStatus(8L, OrderStatus.PLACED)).thenReturn(1L);
		when(orders.countVendorOrdersByStatus(8L, OrderStatus.FULFILLED)).thenReturn(1L);
		when(orders.countVendorOrdersByStatus(8L, OrderStatus.CANCELLED)).thenReturn(0L);
		when(orders.sumVendorUnits(8L)).thenReturn(3L);
		when(orders.sumVendorSales(8L)).thenReturn(new BigDecimal("125.50"));

		assertEquals(List.of(first, second), service.getVendorOrders(8L, Role.VENDOR, pageable).getContent());
		var statistics = service.getVendorStatistics(8L, Role.VENDOR);
		assertEquals(2L, statistics.totalOrders());
		assertEquals(3L, statistics.totalUnits());
		assertEquals(new BigDecimal("125.50"), statistics.totalSales());
	}

	@Test
	void adminStatisticsUseMarketplaceTotals() {
		User admin = new User("admin@example.com", "hash", Role.ADMIN, true);
		setId(admin, 1L);
		when(users.findById(1L)).thenReturn(Optional.of(admin));
		when(orders.countAllOrders()).thenReturn(9L);
		when(orders.countAllOrdersByStatus(OrderStatus.PLACED)).thenReturn(4L);
		when(orders.countAllOrdersByStatus(OrderStatus.FULFILLED)).thenReturn(3L);
		when(orders.countAllOrdersByStatus(OrderStatus.CANCELLED)).thenReturn(2L);
		when(orders.sumAllUnits()).thenReturn(27L);
		when(orders.sumAllSales()).thenReturn(new BigDecimal("900.00"));

		var statistics = service.getAdminStatistics(1L, Role.ADMIN);

		assertEquals(9L, statistics.totalOrders());
		assertEquals(27L, statistics.totalUnits());
		assertEquals(new BigDecimal("900.00"), statistics.totalSales());
	}

	private void setId(User user, Long id) {
		try {
			var field = User.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(user, id);
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError(exception);
		}
	}
}
