package com.marketplace.exception;

import com.marketplace.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.<Void>error("Access denied"));
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvalidCredentialsException(InvalidCredentialsException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.<Void>error("Invalid email or password"));
	}

	@ExceptionHandler(ProductNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleProductNotFoundException(ProductNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Product not found"));
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(UserNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("User not found"));
	}

	@ExceptionHandler(CartItemNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleCartItemNotFoundException(CartItemNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Cart item not found"));
	}

	@ExceptionHandler(CartEmptyException.class)
	public ResponseEntity<ApiResponse<Void>> handleCartEmptyException(CartEmptyException exception) {
		return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Cart is empty"));
	}

	@ExceptionHandler(InsufficientStockException.class)
	public ResponseEntity<ApiResponse<Void>> handleInsufficientStockException(InsufficientStockException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Void>error("Insufficient stock"));
	}

	@ExceptionHandler(OrderNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleOrderNotFoundException(OrderNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Order not found"));
	}

	@ExceptionHandler(OrderCancellationNotAllowedException.class)
	public ResponseEntity<ApiResponse<Void>> handleOrderCancellationNotAllowedException(
			OrderCancellationNotAllowedException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.<Void>error("Order cannot be cancelled"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
			MethodArgumentNotValidException exception) {
		return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Validation failed"));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Invalid request"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.<Void>error("An unexpected error occurred"));
	}
}
