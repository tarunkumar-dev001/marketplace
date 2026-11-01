package com.marketplace.controller;

import com.marketplace.dto.VendorOrderStatisticsResponse;
import com.marketplace.entity.CustomerOrder;
import com.marketplace.enums.Role;
import com.marketplace.exception.GlobalExceptionHandler;
import com.marketplace.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VendorOrderControllerTests {

	private OrderService orderService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		orderService = mock(OrderService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new VendorOrderController(orderService))
				.setControllerAdvice(new GlobalExceptionHandler()).build();
	}

	@Test
	void vendorOrdersUseActorScopeAndPagination() throws Exception {
		CustomerOrder order = mock(CustomerOrder.class);
		when(order.getId()).thenReturn(91L);
		when(order.getItems()).thenReturn(List.of());
		when(orderService.getVendorOrders(eq(8L), eq(Role.VENDOR), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(order), PageRequest.of(1, 5), 6));

		mockMvc.perform(get("/api/vendor/orders").param("page", "1").param("size", "5").principal(principal()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].orderId").value(91))
				.andExpect(jsonPath("$.data.page").value(1));

		verify(orderService).getVendorOrders(eq(8L), eq(Role.VENDOR), any(Pageable.class));
	}

	@Test
	void vendorStatisticsReturnMarketplaceStatsForActor() throws Exception {
		when(orderService.getVendorStatistics(8L, Role.VENDOR))
				.thenReturn(new VendorOrderStatisticsResponse(4, 1, 2, 1, 12, new BigDecimal("125.50")));

		mockMvc.perform(get("/api/vendor/orders/statistics").principal(principal()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalOrders").value(4))
				.andExpect(jsonPath("$.data.totalUnits").value(12))
				.andExpect(jsonPath("$.data.totalSales").value(125.50));
	}

	private JwtAuthenticationToken principal() {
		Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").subject("8")
				.claim("role", "VENDOR").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
		return new JwtAuthenticationToken(jwt);
	}
}
