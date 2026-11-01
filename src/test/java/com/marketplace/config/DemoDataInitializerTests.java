package com.marketplace.config;

import com.marketplace.entity.User;
import com.marketplace.enums.Role;
import com.marketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DemoDataInitializerTests {

	private UserRepository userRepository;
	private PasswordEncoder passwordEncoder;
	private DemoProperties properties;
	private DemoDataInitializer initializer;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		passwordEncoder = mock(PasswordEncoder.class);
		properties = new DemoProperties();
		properties.setAdminEmail("ADMIN@Example.com");
		properties.setVendorEmail("VENDOR@Example.com");
		properties.setCustomerEmail("CUSTOMER@Example.com");
		properties.setAdminPassword("admin-test-password");
		properties.setVendorPassword("vendor-test-password");
		properties.setCustomerPassword("customer-test-password");
		initializer = new DemoDataInitializer(userRepository, passwordEncoder, properties);
	}

	@Test
	void activeOnlyForLocalAndDemoProfiles() {
		Profile profile = DemoDataInitializer.class.getAnnotation(Profile.class);

		assertEquals(1, profile.value().length);
		assertEquals("demo", profile.value()[0]);
	}

	@Test
	void credentialPropertiesHaveNoSourceDefaults() {
		DemoProperties unconfigured = new DemoProperties();

		assertEquals(null, unconfigured.getAdminEmail());
		assertEquals(null, unconfigured.getAdminPassword());
		assertEquals(null, unconfigured.getVendorEmail());
		assertEquals(null, unconfigured.getVendorPassword());
		assertEquals(null, unconfigured.getCustomerEmail());
		assertEquals(null, unconfigured.getCustomerPassword());
	}

	@Test
	void createsMissingUsersWithCanonicalEmailsAndEncodedPasswords() {
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
		when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "bcrypt:" + invocation.getArgument(0));
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		initializer.run(null);

		verify(passwordEncoder).encode(properties.getAdminPassword());
		verify(passwordEncoder).encode(properties.getVendorPassword());
		verify(passwordEncoder).encode(properties.getCustomerPassword());
		org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
		verify(userRepository, org.mockito.Mockito.times(3)).save(captor.capture());
		assertEquals("admin@example.com", captor.getAllValues().get(0).getEmail());
		assertEquals("vendor@example.com", captor.getAllValues().get(1).getEmail());
		assertEquals("customer@example.com", captor.getAllValues().get(2).getEmail());
		assertEquals(Role.ADMIN, captor.getAllValues().get(0).getRole());
		assertEquals(Role.VENDOR, captor.getAllValues().get(1).getRole());
		assertEquals(Role.CUSTOMER, captor.getAllValues().get(2).getRole());
	}

	@Test
	void repeatInitializationNeverMutatesExistingPassword() {
		User existing = new User("admin@example.com", "$2a$existing-byte-for-byte", Role.ADMIN, true);
		String existingHash = existing.getPasswordHash();
		when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));
		when(userRepository.findByEmail("vendor@example.com")).thenReturn(Optional.empty());
		when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode(anyString())).thenReturn("new-hash");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		initializer.run(null);

		assertEquals(existingHash, existing.getPasswordHash());
		verify(passwordEncoder, never()).encode(properties.getAdminPassword());
		verify(passwordEncoder).encode(properties.getVendorPassword());
		verify(passwordEncoder).encode(properties.getCustomerPassword());
	}

	@Test
	void existingInactiveUsersRemainInactiveUnlessActivationPolicyIsEnabled() {
		User existing = new User("admin@example.com", "existing", Role.ADMIN, false);
		when(userRepository.findByEmail(anyString())).thenAnswer(invocation -> invocation.getArgument(0).equals("admin@example.com")
				? Optional.of(existing) : Optional.empty());
		when(passwordEncoder.encode(anyString())).thenReturn("new-hash");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		initializer.run(null);

		assertEquals(false, existing.isActive());

		properties.setActivateExistingUsers(true);
		initializer.run(null);
		assertEquals(true, existing.isActive());
		verify(userRepository).save(existing);
	}

	@Test
	void missingCredentialConfigurationFailsSafely() {
		properties.setAdminPassword(" ");

		assertThrows(IllegalStateException.class, () -> initializer.run(null));
		verifyNoMoreInteractions(userRepository);
	}
}
