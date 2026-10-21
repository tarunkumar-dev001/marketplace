package com.marketplace.controller;

import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.ProductRequest;
import com.marketplace.dto.ProductResponse;
import com.marketplace.entity.Product;
import com.marketplace.enums.Role;
import com.marketplace.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.util.List;
import java.security.Principal;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request,
			Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		Product savedProduct = productService.create(toProduct(request), actorUserId(jwt), actorRole(jwt));
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Product created", ProductResponse.from(savedProduct)));
	}

	@GetMapping
	public ApiResponse<List<ProductResponse>> getAll() {
		List<ProductResponse> products = productService.getAll().stream()
				.map(ProductResponse::from)
				.toList();
		return ApiResponse.success("Products retrieved", products);
	}

	@GetMapping("/{id}")
	public ApiResponse<ProductResponse> getById(@PathVariable Long id) {
		return ApiResponse.success("Product retrieved", ProductResponse.from(productService.getById(id)));
	}

	@PutMapping("/{id}")
	public ApiResponse<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request,
			Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		Product updatedProduct = productService.update(id, toProduct(request), actorUserId(jwt), actorRole(jwt));
		return ApiResponse.success("Product updated", ProductResponse.from(updatedProduct));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		productService.delete(id, actorUserId(jwt), actorRole(jwt));
		return ResponseEntity.noContent().build();
	}

	private Product toProduct(ProductRequest request) {
		return new Product(
				request.name(),
				request.description(),
				request.price(),
				request.stockQuantity(),
				request.category(),
				request.active()
		);
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
