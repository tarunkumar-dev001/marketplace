package com.marketplace.dto;

import com.marketplace.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
	@NotBlank @Size(max=120) String name,
	@NotBlank @Size(max=20) String phone,
	@NotBlank @Size(max=300) String address,
	@NotBlank @Size(max=80) String city,
	@NotBlank @Pattern(regexp="\\d{6}") String pincode,
	@NotNull PaymentMethod paymentMethod
) {}
