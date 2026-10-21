package com.marketplace.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Set;

@Configuration
public class JwtConfig {

	private static final int MINIMUM_SECRET_LENGTH_BYTES = 32;

	@Bean
	SecretKey jwtSecretKey(@Value("${MARKETPLACE_JWT_SECRET_BASE64:}") String secretBase64) {
		if (secretBase64 == null || secretBase64.isBlank()) {
			throw new IllegalStateException("MARKETPLACE_JWT_SECRET_BASE64 must be configured");
		}

		final byte[] keyBytes;
		try {
			keyBytes = Base64.getDecoder().decode(secretBase64);
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("MARKETPLACE_JWT_SECRET_BASE64 must be valid Base64", exception);
		}

		if (keyBytes.length < MINIMUM_SECRET_LENGTH_BYTES) {
			throw new IllegalStateException("MARKETPLACE_JWT_SECRET_BASE64 must decode to at least 32 bytes");
		}
		return new SecretKeySpec(keyBytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		OctetSequenceKey key = new OctetSequenceKey.Builder(jwtSecretKey)
				.algorithm(JWSAlgorithm.HS256)
				.build();
		ImmutableJWKSet<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(key));
		return new NimbusJwtEncoder(jwkSource);
	}

	@Bean
	JwtDecoder jwtDecoder(
			SecretKey jwtSecretKey,
			@Value("${MARKETPLACE_JWT_ISSUER:}") String issuer) {
		String requiredIssuer = requireIssuer(issuer);
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(requiredIssuer), jwtClaimsValidator()));
		return decoder;
	}

	private static OAuth2TokenValidator<Jwt> jwtClaimsValidator() {
		return jwt -> {
			Object subject = jwt.getClaim(JwtClaimNames.SUB);
			Object role = jwt.getClaim("role");
			if (!isPositiveUserId(subject) || !(role instanceof String roleValue)
					|| !Set.of("CUSTOMER", "VENDOR", "ADMIN").contains(roleValue)) {
				return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid token claims", null));
			}
			return OAuth2TokenValidatorResult.success();
		};
	}

	private static boolean isPositiveUserId(Object subject) {
		if (!(subject instanceof String subjectValue) || !subjectValue.matches("[1-9][0-9]*")) {
			return false;
		}
		try {
			return Long.parseLong(subjectValue) > 0;
		} catch (NumberFormatException exception) {
			return false;
		}
	}

	private static String requireIssuer(String issuer) {
		if (issuer == null || issuer.isBlank()) {
			throw new IllegalStateException("MARKETPLACE_JWT_ISSUER must be configured");
		}
		return issuer;
	}
}
