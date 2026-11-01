package com.marketplace.controller;

import com.marketplace.dto.ApiResponse;
import com.marketplace.dto.OrderStatusResponse;
import com.marketplace.dto.OrderStatusesResponse;
import com.marketplace.enums.OrderStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/api/orders")
public class OrderStatusController {

	@GetMapping("/statuses")
	public ApiResponse<OrderStatusesResponse> getStatuses() {
		var statuses = Arrays.stream(OrderStatus.values())
				.map(status -> new OrderStatusResponse(status.name(), displayLabel(status)))
				.toList();
		return ApiResponse.success("Order statuses retrieved", new OrderStatusesResponse(statuses));
	}

	private String displayLabel(OrderStatus status) {
		return switch (status) {
			case PLACED -> "Placed";
			case CANCELLED -> "Cancelled";
			case FULFILLED -> "Fulfilled";
		};
	}
}
