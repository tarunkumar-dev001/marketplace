package com.marketplace.controller;

import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.ProductRequest;
import com.marketplace.dto.ProductPageResponse;
import com.marketplace.dto.ProductResponse;
import com.marketplace.entity.Product;
import com.marketplace.enums.Role;
import com.marketplace.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Locale;
import java.util.Map;

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
		Product savedProduct = productService.create(toProduct(request, true), actorUserId(jwt), actorRole(jwt));
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Product created", ProductResponse.from(savedProduct)));
	}

	@GetMapping
	public ApiResponse<ProductPageResponse> getAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {
		if (page < 0 || size < 1 || size > 100) {
			throw new IllegalArgumentException("Invalid pagination parameters");
		}
		Pageable pageable = PageRequest.of(page, size, sortFor(sortBy, direction));
		Page<ProductResponse> products = productService.getAll(pageable, trimToNull(category), trimToNull(search))
				.map(ProductResponse::from);
		return ApiResponse.success("Products retrieved", ProductPageResponse.from(products));
	}

	@GetMapping("/{id}")
	public ApiResponse<ProductResponse> getById(@PathVariable Long id) {
		return ApiResponse.success("Product retrieved", ProductResponse.from(productService.getById(id)));
	}

	@GetMapping("/mine")
	public ApiResponse<ProductPageResponse> getMine(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			Principal principal) {
		if (page < 0 || size < 1 || size > 100) {
			throw new IllegalArgumentException("Invalid pagination parameters");
		}
		Jwt jwt = authenticatedJwt(principal);
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")
				.and(Sort.by(Sort.Direction.DESC, "id")));
		Page<ProductResponse> products = productService.getMine(pageable, actorUserId(jwt), actorRole(jwt))
				.map(ProductResponse::from);
		return ApiResponse.success("Vendor products retrieved", ProductPageResponse.from(products));
	}

	@PutMapping("/{id}")
	public ApiResponse<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request,
			Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		Product updatedProduct = productService.update(id, toProduct(request, false), actorUserId(jwt), actorRole(jwt));
		return ApiResponse.success("Product updated", ProductResponse.from(updatedProduct));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
		Jwt jwt = authenticatedJwt(principal);
		productService.delete(id, actorUserId(jwt), actorRole(jwt));
		return ResponseEntity.noContent().build();
	}

	private Product toProduct(ProductRequest request, boolean defaultActive) {
		return new Product(
				request.name(),
				request.description(),
				request.price(),
				request.stockQuantity(),
				request.category(),
				request.active() == null ? defaultActive : request.active()
		);
	}

	private Sort sortFor(String sortBy, String direction) {
		String property = Map.of(
				"createdAt", "createdAt",
				"name", "name",
				"price", "price"
		).get(sortBy);
		if (property == null) {
			throw new IllegalArgumentException("Unsupported sort field");
		}
		final Sort.Direction sortDirection;
		try {
			sortDirection = Sort.Direction.valueOf(direction.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new IllegalArgumentException("Unsupported sort direction");
		}
		return Sort.by(sortDirection, property).and(Sort.by(sortDirection, "id"));
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
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
