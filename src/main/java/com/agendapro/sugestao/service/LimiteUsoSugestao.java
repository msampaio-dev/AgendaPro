package com.agendapro.sugestao.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.sugestao.exception.LimiteSugestoesAtingidoException;

/**
 * Conta quantas sugestoes cada usuario pediu no dia, antes de chamar a IA.
 *
 * O contador fica no banco, e nao em memoria como o LoginRateLimiter: o Render
 * desliga a instancia gratuita depois de alguns minutos ociosa, e um contador em
 * memoria zeraria a cada despertar. Aqui o limite protege dinheiro, nao so a
 * aplicacao.
 *
 * O limite por usuario sozinho nao basta, porque o cadastro e aberto e quem
 * quisesse abusar criaria contas novas. O teto global fecha esse caminho; ele e
 * aproximado (duas requisicoes simultaneas podem passar uma unidade do teto),
 * o que e aceitavel para um limite de custo.
 */
@Component
public class LimiteUsoSugestao {

	/** O "dia" do limite segue o horario de quem usa a demonstracao. */
	static final ZoneId FUSO_DO_LIMITE = ZoneId.of("America/Sao_Paulo");

	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;
	private final int limitePorUsuario;
	private final int limiteGlobal;

	public LimiteUsoSugestao(
			JdbcTemplate jdbcTemplate,
			Clock clock,
			@Value("${app.ia.sugestao.limite-por-usuario}") int limitePorUsuario,
			@Value("${app.ia.sugestao.limite-global}") int limiteGlobal
	) {
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
		this.limitePorUsuario = limitePorUsuario;
		this.limiteGlobal = limiteGlobal;
	}

	/**
	 * Registra um uso e devolve o dia contado e quantos usos ainda restam nele.
	 *
	 * O incremento acontece antes da verificacao e dentro da mesma transacao: se
	 * o limite estourar, a excecao desfaz o incremento. O upsert trava a linha do
	 * usuario, entao duas requisicoes dele ao mesmo tempo sao contadas em fila.
	 */
	@Transactional
	public UsoRegistrado registrarUso(Long usuarioId) {
		LocalDate hoje = LocalDate.now(clock.withZone(FUSO_DO_LIMITE));

		Integer usosDoUsuario = jdbcTemplate.queryForObject(
				"INSERT INTO usos_sugestao_agendamento (usuario_id, dia, quantidade) "
				+ "VALUES (?, ?, 1) "
				+ "ON CONFLICT (usuario_id, dia) "
				+ "DO UPDATE SET quantidade = usos_sugestao_agendamento.quantidade + 1 "
				+ "RETURNING quantidade",
				Integer.class,
				usuarioId,
				hoje
		);
		if (usosDoUsuario > limitePorUsuario) {
			throw new LimiteSugestoesAtingidoException();
		}

		Integer usosDoDia = jdbcTemplate.queryForObject(
				"SELECT SUM(quantidade) FROM usos_sugestao_agendamento WHERE dia = ?",
				Integer.class,
				hoje
		);
		if (usosDoDia > limiteGlobal) {
			throw new LimiteSugestoesAtingidoException();
		}

		return new UsoRegistrado(hoje, limitePorUsuario - usosDoUsuario);
	}

	/**
	 * Desfaz um uso que nao chegou a gerar resposta da IA (sem chave, rede fora,
	 * API indisponivel). Sem isso, uma queda da API consumiria a cota do cliente
	 * sem que ele recebesse nada.
	 *
	 * Recebe o dia do registro, e nao o de agora, para que uma chamada que cruza a
	 * meia-noite devolva o uso ao dia em que ele foi contado. A ultima unidade
	 * apaga a linha, porque a tabela nao aceita quantidade zero.
	 */
	@Transactional
	public void devolverUso(Long usuarioId, LocalDate dia) {
		int atualizadas = jdbcTemplate.update(
				"UPDATE usos_sugestao_agendamento SET quantidade = quantidade - 1 "
				+ "WHERE usuario_id = ? AND dia = ? AND quantidade > 1",
				usuarioId,
				dia
		);
		if (atualizadas == 0) {
			jdbcTemplate.update(
					"DELETE FROM usos_sugestao_agendamento "
					+ "WHERE usuario_id = ? AND dia = ? AND quantidade = 1",
					usuarioId,
					dia
			);
		}
	}

	public record UsoRegistrado(LocalDate dia, int restantes) {
	}
}
