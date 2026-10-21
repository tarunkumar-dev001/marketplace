package com.marketplace.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductRequestTests {

	@Test
	void requestPreservesBusinessFields() {
		ProductRequest request = new ProductRequest(
				"Wireless Headphones",
				"Noise-cancelling headphones",
				new BigDecimal("129.99"),
				25,
				"Electronics",
				true
		);

		assertEquals("Wireless Headphones", request.name());
		assertEquals("Noise-cancelling headphones", request.description());
		assertEquals(new BigDecimal("129.99"), request.price());
		assertEquals(25, request.stockQuantity());
		assertEquals("Electronics", request.category());
		assertTrue(request.active());
	}

	@Test
	void validationRejectsInvalidRequiredFields() {
		ProductRequest request = new ProductRequest("", null, BigDecimal.ZERO, -1, "", false);

		try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
			Validator validator = validatorFactory.getValidator();
			Set<String> invalidFields = validator.validate(request).stream()
					.map(violation -> violation.getPropertyPath().toString())
					.collect(java.util.stream.Collectors.toSet());

			assertEquals(Set.of("name", "price", "stockQuantity", "category"), invalidFields);
		}
	}
}
