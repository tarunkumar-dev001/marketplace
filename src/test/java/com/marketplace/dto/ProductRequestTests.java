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
	void requestTrimsNameAndCategory() {
		ProductRequest request = new ProductRequest(
				"  Wireless Headphones  ",
				null,
				new BigDecimal("129.99"),
				25,
				"  Electronics  ",
				null
		);

		assertEquals("Wireless Headphones", request.name());
		assertEquals("Electronics", request.category());
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

	@Test
	void validationRejectsUnsupportedPriceScaleAndPrecision() {
		ProductRequest excessiveScale = new ProductRequest("Name", null, new BigDecimal("1.001"), 1, "Category", true);
		ProductRequest excessivePrecision = new ProductRequest(
				"Name", null, new BigDecimal("100000000000000000.00"), 1, "Category", true);

		try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
			Validator validator = validatorFactory.getValidator();

			assertEquals(Set.of("price"), invalidFields(validator, excessiveScale));
			assertEquals(Set.of("price"), invalidFields(validator, excessivePrecision));
		}
	}

	@Test
	void validationRejectsNameAndCategoryBlankAfterTrimming() {
		ProductRequest request = new ProductRequest("   ", null, new BigDecimal("1.00"), 1, "\t", true);

		try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
			assertEquals(Set.of("name", "category"), invalidFields(validatorFactory.getValidator(), request));
		}
	}

	private Set<String> invalidFields(Validator validator, ProductRequest request) {
		return validator.validate(request).stream()
				.map(violation -> violation.getPropertyPath().toString())
				.collect(java.util.stream.Collectors.toSet());
	}
}
