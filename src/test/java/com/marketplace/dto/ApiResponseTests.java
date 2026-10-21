package com.marketplace.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseTests {

	@Test
	void successFactoryCreatesSuccessfulResponse() {
		ApiResponse<String> response = ApiResponse.success("Request completed", "payload");

		assertTrue(response.success());
		assertEquals("Request completed", response.message());
		assertEquals("payload", response.data());
	}

	@Test
	void errorFactoryCreatesUnsuccessfulResponseWithoutData() {
		ApiResponse<String> response = ApiResponse.error("Request failed");

		assertFalse(response.success());
		assertEquals("Request failed", response.message());
		assertNull(response.data());
	}
}
