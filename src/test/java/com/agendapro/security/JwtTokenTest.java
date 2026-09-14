package com.agendapro.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import com.agendapro.auth.service.TokenGerado;
import com.agendapro.auth.service.TokenService;
import com.agendapro.usuario.entity.Usuario;

class JwtTokenTest {

	private JwtDecoder jwtDecoder;
	private TokenService tokenService;

	@BeforeEach
	void configurarJwt() {
		String segredo = Base64.getEncoder().encodeToString(new byte[32]);
		JwtConfig config = new JwtConfig();
		var secretKey = config.jwtSecretKey(segredo);
		JwtEncoder jwtEncoder = config.jwtEncoder(secretKey);
		jwtDecoder = config.jwtDecoder(secretKey);
		tokenService = new TokenService(jwtEncoder, 60);
	}

	@Test
	void deveGerarTokenAssinadoComIdentificacaoEValidade() {
		Usuario usuario = mock(Usuario.class);
		when(usuario.getId()).thenReturn(42L);
		when(usuario.getEmail()).thenReturn("marcelo@agendapro.com");

		TokenGerado tokenGerado = tokenService.gerar(usuario);
		Jwt jwt = jwtDecoder.decode(tokenGerado.valor());

		assertEquals("agendapro", jwt.getClaimAsString("iss"));
		assertEquals("42", jwt.getSubject());
		assertEquals("marcelo@agendapro.com", jwt.getClaimAsString("email"));
		assertEquals(60, Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toMinutes());
		assertEquals(jwt.getExpiresAt(), tokenGerado.expiraEm());
	}

	@Test
	void naoDeveAceitarTokenAdulterado() {
		Usuario usuario = mock(Usuario.class);
		when(usuario.getId()).thenReturn(42L);
		when(usuario.getEmail()).thenReturn("marcelo@agendapro.com");

		String token = tokenService.gerar(usuario).valor();
		String[] partes = token.split("\\.");
		char primeiroCaractereAssinatura = partes[2].charAt(0);
		char caractereAlterado = primeiroCaractereAssinatura == 'a' ? 'b' : 'a';
		partes[2] = caractereAlterado + partes[2].substring(1);
		String tokenAdulterado = String.join(".", partes);

		assertThrows(JwtException.class, () -> jwtDecoder.decode(tokenAdulterado));
	}

	@Test
	void deveRejeitarSegredoComMenosDe256Bits() {
		String segredoCurto = Base64.getEncoder().encodeToString(new byte[16]);

		assertThrows(IllegalStateException.class, () -> new JwtConfig().jwtSecretKey(segredoCurto));
	}
}
