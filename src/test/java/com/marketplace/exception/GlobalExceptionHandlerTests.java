package com.marketplace.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTests {

	private static final String INTERNAL_DETAIL = "internal-secret-detail-9f3c";

	private LocalValidatorFactoryBean validator;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
				.setControllerAdvice(new GlobalExceptionHandler())
				.setValidator(validator)
				.build();
	}

	@AfterEach
	void tearDown() {
		validator.destroy();
	}

	@Test
	void illegalArgumentExceptionReturnsBadRequestResponse() throws Exception {
		mockMvc.perform(get("/test/illegal-argument").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Invalid request"))
				.andExpect(jsonPath("$.data").value((Object) null));
	}

	@Test
	void unexpectedExceptionReturnsSafeInternalServerErrorResponse() throws Exception {
		mockMvc.perform(get("/test/unexpected").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("An unexpected error occurred"))
				.andExpect(jsonPath("$.data").value((Object) null))
				.andExpect(content().string(not(containsString(INTERNAL_DETAIL))));
	}

	@Test
	void methodArgumentNotValidExceptionReturnsBadRequestValidationErrorResponse() throws Exception {
		mockMvc.perform(post("/test/validation")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\": \" \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.data").value((Object) null));
	}

	@Test
	void productNotFoundExceptionReturnsNotFoundResponse() throws Exception {
		mockMvc.perform(get("/test/product-not-found").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Product not found"))
				.andExpect(jsonPath("$.data").value((Object) null));
	}

	@Test
	void invalidCredentialsExceptionReturnsSafeUnauthorizedResponse() throws Exception {
		mockMvc.perform(get("/test/invalid-credentials").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Invalid email or password"))
				.andExpect(jsonPath("$.data").value((Object) null));
	}

	@Test
	void accessDeniedExceptionReturnsSafeForbiddenResponse() throws Exception {
		mockMvc.perform(get("/test/access-denied").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Access denied"))
				.andExpect(jsonPath("$.data").value((Object) null));
	}

	@Controller
	@RequestMapping("/test")
	static class ThrowingController {

		@GetMapping("/illegal-argument")
		@ResponseBody
		void throwIllegalArgumentException() {
			throw new IllegalArgumentException("Invalid request");
		}

		@GetMapping("/unexpected")
		@ResponseBody
		void throwUnexpectedException() {
			throw new IllegalStateException(INTERNAL_DETAIL);
		}

		@GetMapping("/product-not-found")
		@ResponseBody
		void throwProductNotFoundException() {
			throw new ProductNotFoundException();
		}

		@GetMapping("/invalid-credentials")
		@ResponseBody
		void throwInvalidCredentialsException() {
			throw new InvalidCredentialsException();
		}

		@GetMapping("/access-denied")
		@ResponseBody
		void throwAccessDeniedException() {
			throw new AccessDeniedException("Sensitive authorization detail");
		}

		@PostMapping("/validation")
		@ResponseBody
		void validate(@Valid @RequestBody ValidationRequest request) {
		}
	}

	private record ValidationRequest(@NotBlank String name) {
	}
}
