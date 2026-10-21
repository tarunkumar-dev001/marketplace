package com.marketplace.controller;

import com.marketplace.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

	@GetMapping
	public ApiResponse<Map<String, String>> health() {
		return ApiResponse.success("Service is healthy", Map.of("status", "UP"));
	}
}
