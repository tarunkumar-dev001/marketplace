package com.marketplace.controller;

import com.marketplace.entity.Product;
import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.dto.AdminOrderStatisticsResponse;
import com.marketplace.entity.CustomerOrder;
import com.marketplace.exception.GlobalExceptionHandler;
import com.marketplace.service.OrderService;
import com.marketplace.service.ProductService;
import com.marketplace.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTests {

	private UserServiceImpl userService;
	private ProductService productService;
	private OrderService orderService;
	private LocalValidatorFactoryBean validator;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		userService = mock(UserServiceImpl.class);
		productService = mock(ProductService.class);
		orderService = mock(OrderService.class);
		validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(new AdminController(userService, productService, orderService))
				.setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
	}

	@AfterEach
	void tearDown() {
		validator.destroy();
	}

	@Test
	void getUsersReturnsPaginatedUsersForAdmin() throws Exception {
		User user = user(2L, "customer@example.com", Role.CUSTOMER, false);
		when(userService.getAllForAdmin(any(Pageable.class), eq(1L), eq(Role.ADMIN)))
				.thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/admin/users").principal(principal()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].email").value("customer@example.com"))
				.andExpect(jsonPath("$.data.items[0].active").value(false)).andExpect(jsonPath("$.data.page").value(0));

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(userService).getAllForAdmin(pageable.capture(), eq(1L), eq(Role.ADMIN));
		assertEquals(Sort.Direction.DESC, pageable.getValue().getSort().getOrderFor("createdAt").getDirection());
	}

	@Test
	void updateUserStatusValidatesAndDelegates() throws Exception {
		User user = user(2L, "customer@example.com", Role.CUSTOMER, true);
		when(userService.updateStatus(2L, true, 1L, Role.ADMIN)).thenReturn(user);

		mockMvc.perform(patch("/api/admin/users/{id}/status", 2L).principal(principal())
					.contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.message").value("User status updated"));
		verify(userService).updateStatus(2L, true, 1L, Role.ADMIN);

		mockMvc.perform(patch("/api/admin/users/{id}/status", 2L).principal(principal())
					.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Validation failed"));
	}

	@Test
	void getOrdersUsesAdminActorScopeAndPagination() throws Exception {
		CustomerOrder order = mock(CustomerOrder.class);
		when(order.getId()).thenReturn(42L);
		when(order.getItems()).thenReturn(List.of());
		when(orderService.getAdminOrders(eq(1L), eq(Role.ADMIN), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/admin/orders").principal(principal()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].orderId").value(42))
				.andExpect(jsonPath("$.data.totalElements").value(1));

		verify(orderService).getAdminOrders(eq(1L), eq(Role.ADMIN), any(Pageable.class));
	}

	@Test
	void getOrderStatisticsReturnsMarketplaceTotals() throws Exception {
		when(orderService.getAdminStatistics(1L, Role.ADMIN))
				.thenReturn(new AdminOrderStatisticsResponse(9, 4, 3, 2, 27, new BigDecimal("900.00")));

		mockMvc.perform(get("/api/admin/orders/statistics").principal(principal()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalOrders").value(9))
				.andExpect(jsonPath("$.data.totalUnits").value(27))
				.andExpect(jsonPath("$.data.totalSales").value(900.00));
	}

	@Test
	void getProductsIncludesInactiveProductsAndVendorIdentity() throws Exception {
		User vendor = user(7L, "vendor@example.com", Role.VENDOR, true);
		Product product = mock(Product.class);
		when(product.getId()).thenReturn(3L);
		when(product.getName()).thenReturn("Archived product");
		when(product.getDescription()).thenReturn("Description");
		when(product.getPrice()).thenReturn(new BigDecimal("12.50"));
		when(product.getStockQuantity()).thenReturn(2);
		when(product.getCategory()).thenReturn("Books");
		when(product.isActive()).thenReturn(false);
		when(product.getVendor()).thenReturn(vendor);
		when(productService.getAllForAdmin(any(Pageable.class), eq(1L), eq(Role.ADMIN)))
				.thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/admin/products").principal(principal()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].active").value(false))
				.andExpect(jsonPath("$.data.items[0].vendorId").value(7))
				.andExpect(jsonPath("$.data.items[0].vendorEmail").value("vendor@example.com"));
	}

	@Test
	void updateProductStatusValidatesAndDelegates() throws Exception {
		Product product = mock(Product.class);
		when(product.getId()).thenReturn(3L);
		when(product.getName()).thenReturn("Product");
		when(product.getPrice()).thenReturn(new BigDecimal("12.50"));
		when(product.getStockQuantity()).thenReturn(2);
		when(product.getCategory()).thenReturn("Books");
		when(product.isActive()).thenReturn(false);
		when(productService.updateStatus(3L, false, 1L, Role.ADMIN)).thenReturn(product);

		mockMvc.perform(patch("/api/admin/products/{id}/status", 3L).principal(principal())
					.contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(false));
		verify(productService).updateStatus(3L, false, 1L, Role.ADMIN);
	}

	private User user(long id, String email, Role role, boolean active) {
		User user = mock(User.class);
		when(user.getId()).thenReturn(id);
		when(user.getEmail()).thenReturn(email);
		when(user.getRole()).thenReturn(role);
		when(user.isActive()).thenReturn(active);
		return user;
	}

	private JwtAuthenticationToken principal() {
		Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").subject("1").claim("role", "ADMIN")
				.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
		return new JwtAuthenticationToken(jwt);
	}
}
