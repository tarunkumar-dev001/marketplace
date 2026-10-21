package com.marketplace.security;

import com.marketplace.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

	private final JwtEncoder jwtEncoder;
	private final String issuer;
	private final Duration accessTokenLifetime;

	public JwtService(
			JwtEncoder jwtEncoder,
			@Value("${MARKETPLACE_JWT_ISSUER:}") String issuer,
			@Value("${MARKETPLACE_JWT_ACCESS_TOKEN_TTL_SECONDS:900}") long accessTokenLifetimeSeconds) {
		if (issuer == null || issuer.isBlank()) {
			throw new IllegalArgumentException("JWT issuer must not be blank");
		}
		if (accessTokenLifetimeSeconds <= 0) {
			throw new IllegalArgumentException("JWT access token lifetime must be positive");
		}
		this.jwtEncoder = jwtEncoder;
		this.issuer = issuer;
		this.accessTokenLifetime = Duration.ofSeconds(accessTokenLifetimeSeconds);
	}

	public IssuedToken issueAccessToken(User user) {
		if (user.getId() == null) {
			throw new IllegalArgumentException("User ID must not be null when issuing a token");
		}
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(accessTokenLifetime);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.subject(user.getId().toString())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("email", user.getEmail())
				.claim("role", user.getRole().name())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new IssuedToken(token, accessTokenLifetime.getSeconds());
	}

	public record IssuedToken(String token, long expiresInSeconds) {
	}
}
