package com.agendapro.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.agendapro.auth.exception.LoginBloqueadoException;

/**
 * Limitador de tentativas de login em memória. Protege contra brute-force e
 * enumeração de e-mail no /auth/login. Escopo por instância da aplicação:
 * suficiente para o deployment atual (instância única); num cenário
 * multi-instância isso precisaria migrar para um estado compartilhado (ex.: Redis).
 */
@Component
public class LoginRateLimiter {

	private static final int MAX_TENTATIVAS = 5;
	private static final Duration JANELA_TENTATIVAS = Duration.ofMinutes(15);
	private static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(15);

	private final ConcurrentHashMap<String, Tentativas> tentativasPorChave = new ConcurrentHashMap<>();
	private final Clock clock;

	public LoginRateLimiter(Clock clock) {
		this.clock = clock;
	}

	public void verificarBloqueio(String chave) {
		Tentativas tentativas = tentativasPorChave.get(normalizar(chave));
		if (tentativas != null && tentativas.bloqueadoAte != null
				&& tentativas.bloqueadoAte.isAfter(clock.instant())) {
			throw new LoginBloqueadoException();
		}
	}

	public void registrarFalha(String chave) {
		Instant agora = clock.instant();
		tentativasPorChave.compute(normalizar(chave), (k, atual) -> {
			if (atual == null || Duration.between(atual.ultimaFalha, agora).compareTo(JANELA_TENTATIVAS) > 0) {
				atual = new Tentativas();
			}
			atual.falhas++;
			atual.ultimaFalha = agora;
			if (atual.falhas >= MAX_TENTATIVAS) {
				atual.bloqueadoAte = agora.plus(DURACAO_BLOQUEIO);
				atual.falhas = 0;
			}
			return atual;
		});
	}

	public void registrarSucesso(String chave) {
		tentativasPorChave.remove(normalizar(chave));
	}

	private String normalizar(String chave) {
		return chave.trim().toLowerCase(java.util.Locale.ROOT);
	}

	private static final class Tentativas {
		int falhas;
		Instant ultimaFalha;
		Instant bloqueadoAte;
	}
}
