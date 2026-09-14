package com.agendapro.security;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class JwtConfig {

	@Bean
	SecretKey jwtSecretKey(@Value("${app.security.jwt.secret}") String segredoBase64) {
		byte[] segredo;

		try {
			segredo = Base64.getDecoder().decode(segredoBase64);
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("JWT_SECRET deve estar em Base64", exception);
		}

		if (segredo.length < 32) {
			throw new IllegalStateException("JWT_SECRET deve possuir pelo menos 256 bits");
		}

		return new SecretKeySpec(segredo, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey secretKey) {
		ImmutableSecret<SecurityContext> chave = new ImmutableSecret<>(secretKey);
		return new NimbusJwtEncoder(chave);
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey secretKey) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("agendapro"));
		return decoder;
	}
}
