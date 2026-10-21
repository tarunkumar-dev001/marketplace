package com.marketplace.dto;

import com.marketplace.entity.User;
import com.marketplace.enums.Role;

public record LoginResponse(
		String accessToken,
		String tokenType,
		long expiresIn,
		Long id,
		String email,
		Role role,
		boolean active
) {

	public static LoginResponse from(User user, String accessToken, long expiresIn) {
		return new LoginResponse(
				accessToken,
				"Bearer",
				expiresIn,
				user.getId(),
				user.getEmail(),
				user.getRole(),
				user.isActive()
		);
	}
}
