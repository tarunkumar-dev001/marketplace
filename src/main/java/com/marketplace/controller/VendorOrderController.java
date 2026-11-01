package com.marketplace.controller;

import com.marketplace.entity.CustomerOrder;
import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.OrderPageResponse;
import com.marketplace.dto.OrderResponse;
import com.marketplace.dto.VendorOrderStatisticsResponse;
import com.marketplace.enums.Role;
import com.marketplace.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import com.marketplace.dto.UpdateFulfillmentStatusRequest;

import java.security.Principal;

@RestController
@RequestMapping("/api/vendor/orders")
public class VendorOrderController {

	private static final int MAX_PAGE_SIZE = 100;
	private final OrderService orderService;

	public VendorOrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping
	public ApiResponse<OrderPageResponse> getOrders(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		Page<com.marketplace.entity.CustomerOrder> orders = orderService.getVendorOrders(actorUserId(jwt),
				actorRole(jwt), pageable(page, size));
		Page<OrderResponse> response = orders.map(order -> OrderResponse.from(order, actorUserId(jwt)));
		return ApiResponse.success("Vendor orders retrieved", OrderPageResponse.from(response));
	}

	@GetMapping("/statistics")
	public ApiResponse<VendorOrderStatisticsResponse> getStatistics(Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		return ApiResponse.success("Vendor order statistics retrieved",
				orderService.getVendorStatistics(actorUserId(jwt), actorRole(jwt)));
	}

	@PatchMapping("/{orderId}/items/{itemId}/status")
	public ApiResponse<OrderResponse> updateItemStatus(@PathVariable Long orderId, @PathVariable Long itemId,
			@Valid @RequestBody UpdateFulfillmentStatusRequest request, Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		CustomerOrder order = orderService.updateFulfillmentStatus(orderId, itemId, request.status(),
				actorUserId(jwt), actorRole(jwt));
		return ApiResponse.success("Order item status updated", OrderResponse.from(order, actorUserId(jwt)));
	}

	private org.springframework.data.domain.Pageable pageable(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Invalid pagination parameters");
		}
		return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")
				.and(Sort.by(Sort.Direction.DESC, "id")));
	}

	private Jwt authenticatedJwt(Principal principal) {
		if (!(principal instanceof JwtAuthenticationToken authentication)) {
			throw new AccessDeniedException("Access denied");
		}
		return authentication.getToken();
	}

	private Long actorUserId(Jwt jwt) {
		try {
			long id = Long.parseLong(jwt.getSubject());
			if (id <= 0) {
				throw throwAccessDenied();
			}
			return id;
		} catch (NumberFormatException exception) {
			throw throwAccessDenied();
		}
	}

	private Role actorRole(Jwt jwt) {
		try {
			return Role.valueOf(jwt.getClaimAsString("role"));
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw throwAccessDenied();
		}
	}

	private AccessDeniedException throwAccessDenied() {
		return new AccessDeniedException("Access denied");
	}
}
