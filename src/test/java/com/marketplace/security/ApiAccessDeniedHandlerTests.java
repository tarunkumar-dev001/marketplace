package com.marketplace.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiAccessDeniedHandlerTests {

	@Test
	void handleReturnsSafeForbiddenApiResponse() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		new ApiAccessDeniedHandler().handle(
				new MockHttpServletRequest(), response, new AccessDeniedException("internal detail"));

		assertEquals(403, response.getStatus());
		assertTrue(response.getContentAsString().contains("\"success\":false"));
		assertTrue(response.getContentAsString().contains("\"message\":\"Access denied\""));
		assertTrue(!response.getContentAsString().contains("internal detail"));
	}
}
