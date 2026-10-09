package com.agendapro.sugestao.ia;

import java.time.LocalDate;
import java.util.List;

/**
 * O que a IA pode escolher. Ela recebe a lista fechada de barbearias,
 * profissionais e servicos ativos e devolve apenas ids dessa lista; sem a data de
 * hoje, "sexta a tarde" nao teria como virar uma data.
 */
public record CatalogoSugestao(
		LocalDate hoje,
		List<Barbearia> barbearias
) {

	public record Barbearia(
			Long id,
			String nome,
			List<Profissional> profissionais,
			List<Servico> servicos
	) {
	}

	public record Profissional(
			Long id,
			String nome,
			List<Long> servicoIds
	) {
	}

	public record Servico(
			Long id,
			String nome,
			int duracaoMinutos
	) {
	}
}
