package com.marketplace.controller;

import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.LoginRequest;
import com.marketplace.dto.LoginResponse;
import com.marketplace.dto.RegisterRequest;
import com.marketplace.dto.UserResponse;
import com.marketplace.entity.User;
import com.marketplace.service.AuthService;
import com.marketplace.service.impl.UserServiceImpl;
import com.marketplace.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UserServiceImpl userService;
	private final AuthService authService;
	private final JwtService jwtService;

	public AuthController(UserServiceImpl userService, AuthService authService, JwtService jwtService) {
		this.userService = userService;
		this.authService = authService;
		this.jwtService = jwtService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
		User user = userService.register(request.email(), request.password());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Registration successful", UserResponse.from(user)));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
		User user = authService.authenticate(request.email(), request.password());
		JwtService.IssuedToken token = jwtService.issueAccessToken(user);
		return ResponseEntity.ok(ApiResponse.success(
				"Login successful", LoginResponse.from(user, token.token(), token.expiresInSeconds())));
	}
}
