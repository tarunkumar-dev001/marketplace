package com.marketplace.controller;

import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.CartItemAddRequest;
import com.marketplace.dto.CartItemQuantityRequest;
import com.marketplace.dto.CartItemResponse;
import com.marketplace.dto.CartResponse;
import com.marketplace.dto.CheckoutResponse;
import com.marketplace.enums.Role;
import com.marketplace.service.CartService;
import com.marketplace.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/cart")
public class CartController {

	private final CartService cartService;
	private final CheckoutService checkoutService;

	public CartController(CartService cartService, CheckoutService checkoutService) {
		this.cartService = cartService;
		this.checkoutService = checkoutService;
	}

	@GetMapping
	public ApiResponse<CartResponse> getCart(Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		return ApiResponse.success("Cart retrieved", CartResponse.from(
				cartService.getCart(actorUserId(jwt), actorRole(jwt))));
	}

	@PostMapping("/items")
	public ApiResponse<CartResponse> addItem(@Valid @RequestBody CartItemAddRequest request, Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		return ApiResponse.success("Item added to cart", CartResponse.from(
				cartService.addItem(request.productId(), request.quantity(), actorUserId(jwt), actorRole(jwt))));
	}

	@PutMapping("/items/{itemId}")
	public ApiResponse<CartItemResponse> updateItem(@PathVariable Long itemId,
			@Valid @RequestBody CartItemQuantityRequest request, Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		return ApiResponse.success("Cart item updated", CartItemResponse.from(
				cartService.updateItem(itemId, request.quantity(), actorUserId(jwt), actorRole(jwt))));
	}

	@DeleteMapping("/items/{itemId}")
	public ResponseEntity<Void> removeItem(@PathVariable Long itemId, Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		cartService.removeItem(itemId, actorUserId(jwt), actorRole(jwt));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/checkout")
	public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Checkout completed",
				CheckoutResponse.from(checkoutService.checkout(actorUserId(jwt), actorRole(jwt)))));
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
