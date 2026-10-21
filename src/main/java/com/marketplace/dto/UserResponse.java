package com.marketplace.dto;

import com.marketplace.entity.User;
import com.marketplace.enums.Role;

import java.time.Instant;

public record UserResponse(
		Long id,
		String email,
		Role role,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getRole(),
				user.isActive(),
				user.getCreatedAt(),
				user.getUpdatedAt()
		);
	}
}
