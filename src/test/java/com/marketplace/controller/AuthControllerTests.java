package com.marketplace.controller;

import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.exception.GlobalExceptionHandler;
import com.marketplace.exception.InvalidCredentialsException;
import com.marketplace.service.AuthService;
import com.marketplace.service.impl.UserServiceImpl;
import com.marketplace.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTests {

	private static final Instant CREATED_AT = Instant.parse("2026-01-15T10:15:30Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-01-16T11:20:45Z");
	private static final String VALID_REQUEST = """
			{
			  "email": "customer@example.com",
			  "password": "SafePassword123!"
			}
			""";

	private UserServiceImpl userService;
	private AuthService authService;
	private JwtService jwtService;
	private LocalValidatorFactoryBean validator;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		userService = mock(UserServiceImpl.class);
		authService = mock(AuthService.class);
		jwtService = mock(JwtService.class);
		validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(userService, authService, jwtService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.setValidator(validator)
				.build();
	}

	@AfterEach
	void tearDown() {
		validator.destroy();
	}

	@Test
	void registerReturnsCreatedSafeUserResponse() throws Exception {
		User user = user();
		when(userService.register("customer@example.com", "SafePassword123!")).thenReturn(user);

		mockMvc.perform(post("/api/auth/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content(VALID_REQUEST))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Registration successful"))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.email").value("customer@example.com"))
				.andExpect(jsonPath("$.data.role").value("CUSTOMER"))
				.andExpect(jsonPath("$.data.active").value(true))
				.andExpect(jsonPath("$.data.createdAt").value(CREATED_AT.toString()))
				.andExpect(jsonPath("$.data.updatedAt").value(UPDATED_AT.toString()))
				.andExpect(jsonPath("$.data.password").doesNotExist())
				.andExpect(jsonPath("$.data.passwordHash").doesNotExist());

		verify(userService).register("customer@example.com", "SafePassword123!");
		verifyNoInteractions(authService);
		verifyNoInteractions(jwtService);
	}

	@Test
	void registerReturnsValidationErrorForInvalidRequest() throws Exception {
		mockMvc.perform(post("/api/auth/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\": \"invalid\", \"password\": \"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.data").value((Object) null));

		verifyNoInteractions(userService);
		verifyNoInteractions(authService);
		verifyNoInteractions(jwtService);
	}

	@Test
	void registerReturnsSafeClientErrorForDuplicateEmail() throws Exception {
		when(userService.register("customer@example.com", "SafePassword123!"))
				.thenThrow(new IllegalArgumentException("Email is already registered"));

		mockMvc.perform(post("/api/auth/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content(VALID_REQUEST))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Invalid request"))
				.andExpect(jsonPath("$.data").value((Object) null))
				.andExpect(content().string(not(containsString("already registered"))));

		verify(userService).register("customer@example.com", "SafePassword123!");
		verifyNoInteractions(authService);
		verifyNoInteractions(jwtService);
	}

	@Test
	void loginReturnsSafeUserResponse() throws Exception {
		User user = user();
		when(authService.authenticate("customer@example.com", "SafePassword123!")).thenReturn(user);
		when(jwtService.issueAccessToken(user)).thenReturn(new JwtService.IssuedToken("test-access-token", 900));

		mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(VALID_REQUEST))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Login successful"))
				.andExpect(jsonPath("$.data.accessToken").value("test-access-token"))
				.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.expiresIn").value(900))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.email").value("customer@example.com"))
				.andExpect(jsonPath("$.data.role").value("CUSTOMER"))
				.andExpect(jsonPath("$.data.active").value(true))
				.andExpect(jsonPath("$.data.password").doesNotExist())
				.andExpect(jsonPath("$.data.passwordHash").doesNotExist());

		verify(authService).authenticate("customer@example.com", "SafePassword123!");
		verify(jwtService).issueAccessToken(user);
		verifyNoInteractions(userService);
	}

	@Test
	void loginReturnsGenericUnauthorizedResponseForWrongPassword() throws Exception {
		when(authService.authenticate("customer@example.com", "WrongPassword123!"))
				.thenThrow(new InvalidCredentialsException());

		mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\": \"customer@example.com\", \"password\": \"WrongPassword123!\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Invalid email or password"))
				.andExpect(jsonPath("$.data").value((Object) null));

		verify(authService).authenticate("customer@example.com", "WrongPassword123!");
		verifyNoInteractions(userService);
		verifyNoInteractions(jwtService);
	}

	@Test
	void loginReturnsSameUnauthorizedResponseForUnknownOrInactiveAccount() throws Exception {
		when(authService.authenticate("unknown@example.com", "SafePassword123!"))
				.thenThrow(new InvalidCredentialsException());
		when(authService.authenticate("inactive@example.com", "SafePassword123!"))
				.thenThrow(new InvalidCredentialsException());

		for (String email : new String[]{"unknown@example.com", "inactive@example.com"}) {
			mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"" + email + "\", \"password\": \"SafePassword123!\"}"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.message").value("Invalid email or password"))
					.andExpect(jsonPath("$.data").value((Object) null));
		}

		verify(authService).authenticate("unknown@example.com", "SafePassword123!");
		verify(authService).authenticate("inactive@example.com", "SafePassword123!");
		verifyNoInteractions(userService);
	}

	@Test
	void loginReturnsValidationErrorForInvalidRequest() throws Exception {
		mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\": \"invalid\", \"password\": \"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.data").value((Object) null));

		verifyNoInteractions(userService, authService);
		verifyNoInteractions(jwtService);
	}

	private User user() {
		User user = mock(User.class);
		when(user.getId()).thenReturn(1L);
		when(user.getEmail()).thenReturn("customer@example.com");
		when(user.getRole()).thenReturn(Role.CUSTOMER);
		when(user.isActive()).thenReturn(true);
		when(user.getCreatedAt()).thenReturn(CREATED_AT);
		when(user.getUpdatedAt()).thenReturn(UPDATED_AT);
		return user;
	}
}
