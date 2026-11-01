package com.marketplace.controller;

import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.OrderPageResponse;
import com.marketplace.dto.OrderResponse;
import com.marketplace.entity.CustomerOrder;
import com.marketplace.enums.Role;
import com.marketplace.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private static final int MAX_PAGE_SIZE = 100;

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping
	public ApiResponse<OrderPageResponse> getOrders(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			Principal principal) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Invalid pagination parameters");
		}
		Jwt jwt = authenticatedJwt(principal);
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")
				.and(Sort.by(Sort.Direction.DESC, "id")));
		Page<OrderResponse> orders = orderService.getOrders(actorUserId(jwt), actorRole(jwt), pageable)
				.map(OrderResponse::from);
		return ApiResponse.success("Orders retrieved", OrderPageResponse.from(orders));
	}

	@GetMapping("/{orderId}")
	public ApiResponse<OrderResponse> getOrder(@PathVariable Long orderId, Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		CustomerOrder order = orderService.getOrder(orderId, actorUserId(jwt), actorRole(jwt));
		return ApiResponse.success("Order retrieved", OrderResponse.from(order));
	}

	@PostMapping("/{orderId}/cancel")
	public ApiResponse<OrderResponse> cancelOrder(@PathVariable Long orderId, Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		CustomerOrder order = orderService.cancelOrder(orderId, actorUserId(jwt), actorRole(jwt));
		return ApiResponse.success("Order cancelled", OrderResponse.from(order));
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
			if (userId <= 0) {
				throw accessDenied();
			}
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
