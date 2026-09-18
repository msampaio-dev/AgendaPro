package com.agendapro.auth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agendapro.auth.exception.LoginBloqueadoException;

@ExtendWith(MockitoExtension.class)
class LoginRateLimiterTest {

	private static final Instant INICIO = Instant.parse("2030-01-01T12:00:00Z");
	private static final String CHAVE = "alguem@teste.com";

	@Mock
	private Clock clock;

	private Instant agora;
	private LoginRateLimiter rateLimiter;

	@BeforeEach
	void configurar() {
		agora = INICIO;
		when(clock.instant()).thenAnswer(invocacao -> agora);
		rateLimiter = new LoginRateLimiter(clock);
	}

	@Test
	void naoBloqueiaAntesDoLimiteDeTentativas() {
		for (int i = 0; i < 4; i++) {
			rateLimiter.registrarFalha(CHAVE);
		}

		assertDoesNotThrow(() -> rateLimiter.verificarBloqueio(CHAVE));
	}

	@Test
	void bloqueiaAposCincoFalhasSeguidas() {
		for (int i = 0; i < 5; i++) {
			rateLimiter.registrarFalha(CHAVE);
		}

		assertThrows(LoginBloqueadoException.class, () -> rateLimiter.verificarBloqueio(CHAVE));
	}

	@Test
	void liberaAcessoAposDuracaoDoBloqueioExpirar() {
		for (int i = 0; i < 5; i++) {
			rateLimiter.registrarFalha(CHAVE);
		}
		assertThrows(LoginBloqueadoException.class, () -> rateLimiter.verificarBloqueio(CHAVE));

		agora = agora.plusSeconds(16 * 60);

		assertDoesNotThrow(() -> rateLimiter.verificarBloqueio(CHAVE));
	}

	@Test
	void sucessoLimpaContadorDeFalhas() {
		for (int i = 0; i < 4; i++) {
			rateLimiter.registrarFalha(CHAVE);
		}
		rateLimiter.registrarSucesso(CHAVE);

		for (int i = 0; i < 4; i++) {
			rateLimiter.registrarFalha(CHAVE);
		}

		assertDoesNotThrow(() -> rateLimiter.verificarBloqueio(CHAVE));
	}

	@Test
	void chavesDiferentesNaoInterferemEntreSi() {
		for (int i = 0; i < 5; i++) {
			rateLimiter.registrarFalha(CHAVE);
		}

		assertDoesNotThrow(() -> rateLimiter.verificarBloqueio("outra@teste.com"));
	}
}
