package com.marketplace.controller;

import com.marketplace.dto.AdminProductPageResponse;
import com.marketplace.dto.AdminProductResponse;
import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.StatusUpdateRequest;
import com.marketplace.dto.UserPageResponse;
import com.marketplace.dto.UserResponse;
import com.marketplace.dto.AdminOrderStatisticsResponse;
import com.marketplace.dto.OrderPageResponse;
import com.marketplace.dto.OrderResponse;
import com.marketplace.entity.Product;
import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.service.ProductService;
import com.marketplace.service.OrderService;
import com.marketplace.service.impl.UserServiceImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private static final int MAX_PAGE_SIZE = 100;
	private final UserServiceImpl userService;
	private final ProductService productService;
	private final OrderService orderService;

	public AdminController(UserServiceImpl userService, ProductService productService, OrderService orderService) {
		this.userService = userService;
		this.productService = productService;
		this.orderService = orderService;
	}

	@GetMapping("/orders")
	public ApiResponse<OrderPageResponse> getOrders(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		Page<OrderResponse> orders = orderService.getAdminOrders(actorUserId(jwt), actorRole(jwt), pageable(page, size))
				.map(OrderResponse::from);
		return ApiResponse.success("Admin orders retrieved", OrderPageResponse.from(orders));
	}

	@GetMapping("/orders/statistics")
	public ApiResponse<AdminOrderStatisticsResponse> getOrderStatistics(Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		return ApiResponse.success("Admin order statistics retrieved",
				orderService.getAdminStatistics(actorUserId(jwt), actorRole(jwt)));
	}

	@GetMapping("/users")
	public ApiResponse<UserPageResponse> getUsers(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, Principal principal) {
		Pageable pageable = pageable(page, size);
		Jwt jwt = authenticatedJwt(principal);
		Page<UserResponse> users = userService.getAllForAdmin(pageable, actorUserId(jwt), actorRole(jwt)).map(UserResponse::from);
		return ApiResponse.success("Users retrieved", UserPageResponse.from(users));
	}

	@PatchMapping("/users/{id}/status")
	public ApiResponse<UserResponse> updateUserStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request,
			Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		User user = userService.updateStatus(id, request.active(), actorUserId(jwt), actorRole(jwt));
		return ApiResponse.success("User status updated", UserResponse.from(user));
	}

	@GetMapping("/products")
	public ApiResponse<AdminProductPageResponse> getProducts(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, Principal principal) {
		Pageable pageable = pageable(page, size);
		Jwt jwt = authenticatedJwt(principal);
		Page<AdminProductResponse> products = productService.getAllForAdmin(pageable, actorUserId(jwt), actorRole(jwt))
				.map(AdminProductResponse::from);
		return ApiResponse.success("Admin products retrieved", AdminProductPageResponse.from(products));
	}

	@PatchMapping("/products/{id}/status")
	public ApiResponse<AdminProductResponse> updateProductStatus(@PathVariable Long id,
			@Valid @RequestBody StatusUpdateRequest request, Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		Product product = productService.updateStatus(id, request.active(), actorUserId(jwt), actorRole(jwt));
		return ApiResponse.success("Product status updated", AdminProductResponse.from(product));
	}

	private Pageable pageable(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Invalid pagination parameters");
		}
		return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
	}

	private Jwt authenticatedJwt(Principal principal) {
		if (!(principal instanceof JwtAuthenticationToken authentication)) {
			throw new AccessDeniedException("Access denied");
		}
		return authentication.getToken();
	}

	private Long actorUserId(Jwt jwt) {
		try {
			long userId = Long.parseLong(jwt.getSubject());
			if (userId <= 0) throw accessDenied();
			return userId;
		} catch (NumberFormatException exception) {
			throw accessDenied();
		}
	}

	private Role actorRole(Jwt jwt) {
		try {
			return Role.valueOf(jwt.getClaimAsString("role"));
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw accessDenied();
		}
	}

	private AccessDeniedException accessDenied() {
		return new AccessDeniedException("Access denied");
	}
}
