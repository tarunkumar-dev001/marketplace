package com.marketplace.controller;

import com.marketplace.entity.CustomerOrder;
import com.marketplace.entity.OrderItem;
import com.marketplace.entity.Product;
import com.marketplace.enums.OrderStatus;
import com.marketplace.enums.Role;
import com.marketplace.exception.GlobalExceptionHandler;
import com.marketplace.exception.OrderCancellationNotAllowedException;
import com.marketplace.exception.OrderNotFoundException;
import com.marketplace.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTests {

	private OrderService orderService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		orderService = mock(OrderService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void getOrdersReturnsNewestFirstPageAndUsesSafeDefaultSort() throws Exception {
		CustomerOrder order = order();
		when(orderService.getOrders(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq(Role.CUSTOMER),
				org.mockito.ArgumentMatchers.any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(order), org.springframework.data.domain.PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/orders").principal(principal()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Orders retrieved"))
				.andExpect(jsonPath("$.data.page").value(0))
				.andExpect(jsonPath("$.data.size").value(20))
				.andExpect(jsonPath("$.data.items[0].orderId").value(10))
				.andExpect(jsonPath("$.data.items[0].items[0].productName").value("Purchased Mouse"));

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(orderService).getOrders(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq(Role.CUSTOMER),
				pageable.capture());
		assertEquals("createdAt: DESC,id: DESC", pageable.getValue().getSort().toString());
	}

	@Test
	void getOrdersSupportsPaginationAndRejectsInvalidBounds() throws Exception {
		when(orderService.getOrders(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq(Role.CUSTOMER),
				org.mockito.ArgumentMatchers.any(Pageable.class)))
				.thenReturn(Page.empty(org.springframework.data.domain.PageRequest.of(1, 5)));

		mockMvc.perform(get("/api/orders?page=1&size=5").principal(principal()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.page").value(1))
				.andExpect(jsonPath("$.data.size").value(5));
		mockMvc.perform(get("/api/orders?page=-1").principal(principal()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid request"));
		mockMvc.perform(get("/api/orders?size=101").principal(principal()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid request"));
	}

	@Test
	void getOrderReturnsSnapshotsAndHidesMissingOrForeignOrders() throws Exception {
		CustomerOrder order = order();
		when(orderService.getOrder(10L, 7L, Role.CUSTOMER)).thenReturn(order);

		mockMvc.perform(get("/api/orders/{orderId}", 10L).principal(principal()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Order retrieved"))
				.andExpect(jsonPath("$.data.status").value("PLACED"))
				.andExpect(jsonPath("$.data.currency").value("INR"))
				.andExpect(jsonPath("$.data.totalAmount").value(21.00))
				.andExpect(jsonPath("$.data.items[0].unitPrice").value(10.50))
				.andExpect(jsonPath("$.data.items[0].lineTotal").value(21.00));

		when(orderService.getOrder(99L, 7L, Role.CUSTOMER)).thenThrow(new OrderNotFoundException());
		mockMvc.perform(get("/api/orders/{orderId}", 99L).principal(principal()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Order not found"));
	}

	@Test
	void cancelOrderReturnsTheCancelledOrderAndMapsConflictAndNotFoundErrors() throws Exception {
		CustomerOrder cancelledOrder = order();
		when(cancelledOrder.getStatus()).thenReturn(OrderStatus.CANCELLED);
		when(orderService.cancelOrder(10L, 7L, Role.CUSTOMER)).thenReturn(cancelledOrder);

		mockMvc.perform(post("/api/orders/{orderId}/cancel", 10L).principal(principal()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Order cancelled"))
				.andExpect(jsonPath("$.data.orderId").value(10))
				.andExpect(jsonPath("$.data.status").value("CANCELLED"));

		when(orderService.cancelOrder(11L, 7L, Role.CUSTOMER))
				.thenThrow(new OrderCancellationNotAllowedException());
		mockMvc.perform(post("/api/orders/{orderId}/cancel", 11L).principal(principal()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Order cannot be cancelled"));

		when(orderService.cancelOrder(12L, 7L, Role.CUSTOMER)).thenThrow(new OrderNotFoundException());
		mockMvc.perform(post("/api/orders/{orderId}/cancel", 12L).principal(principal()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Order not found"));
	}

	private CustomerOrder order() {
		Product product = mock(Product.class);
		when(product.getId()).thenReturn(3L);
		OrderItem item = mock(OrderItem.class);
		when(item.getProduct()).thenReturn(product);
		when(item.getProductName()).thenReturn("Purchased Mouse");
		when(item.getUnitPrice()).thenReturn(new BigDecimal("10.50"));
		when(item.getQuantity()).thenReturn(2);
		when(item.getLineTotal()).thenReturn(new BigDecimal("21.00"));
		CustomerOrder order = mock(CustomerOrder.class);
		when(order.getId()).thenReturn(10L);
		when(order.getStatus()).thenReturn(OrderStatus.PLACED);
		when(order.getCurrency()).thenReturn("INR");
		when(order.getTotalAmount()).thenReturn(new BigDecimal("21.00"));
		when(order.getCreatedAt()).thenReturn(Instant.parse("2026-01-15T10:15:30Z"));
		when(order.getUpdatedAt()).thenReturn(Instant.parse("2026-01-15T10:15:30Z"));
		when(order.getItems()).thenReturn(List.of(item));
		return order;
	}

	private org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken principal() {
		org.springframework.security.oauth2.jwt.Jwt jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
				.header("alg", "none")
				.subject("7")
				.claim("role", Role.CUSTOMER.name())
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
		return new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt);
	}
}
