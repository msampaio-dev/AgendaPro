package com.agendapro.agendamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.agendapro.agendamento.entity.StatusAgendamento;
import com.agendapro.agendamento.dto.AgendamentoResponse;
import com.agendapro.agendamento.service.AgendamentoService;
import com.agendapro.shared.PostgresIntegrationTest;

@SpringBootTest
class AgendamentoConcorrenciaIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private AgendamentoService agendamentoService;

	private Long clienteId;
	private Long profissionalId;
	private Long servicoId;
	private Long barbeariaId;

	@BeforeEach
	void prepararDados() {
		jdbcTemplate.execute(
				"TRUNCATE TABLE agendamentos, profissionais_servicos, "
				+ "excecoes_disponibilidade, horarios_atendimento, "
				+ "profissionais, servicos, usuarios, barbearias RESTART IDENTITY CASCADE"
		);

		clienteId = inserirUsuario("Cliente", "cliente@teste.com");
		Long usuarioProfissionalId = inserirUsuario(
				"Profissional",
				"profissional@teste.com"
		);
		barbeariaId = jdbcTemplate.queryForObject(
				"INSERT INTO barbearias (nome, ativo) VALUES ('Teste', TRUE) RETURNING id",
				Long.class
		);
		profissionalId = jdbcTemplate.queryForObject(
				"INSERT INTO profissionais (usuario_id, barbearia_id, ativo, fuso_horario) "
				+ "VALUES (?, ?, TRUE, 'America/Sao_Paulo') RETURNING id",
				Long.class,
				usuarioProfissionalId,
				barbeariaId
		);
		servicoId = jdbcTemplate.queryForObject(
				"INSERT INTO servicos "
				+ "(barbearia_id, nome, descricao, duracao_minutos, preco, ativo) "
				+ "VALUES (?, 'Teste concorrente', NULL, 30, 50.00, TRUE) "
				+ "RETURNING id",
				Long.class,
				barbeariaId
		);
	}

	@Test
	void devePermitirSomenteUmAgendamentoNoMesmoHorario() throws Exception {
		int quantidade = 2;
		CountDownLatch prontas = new CountDownLatch(quantidade);
		CountDownLatch iniciar = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(quantidade);
		List<Future<Boolean>> resultados = new ArrayList<>();

		try {
			for (int indice = 0; indice < quantidade; indice++) {
				resultados.add(executor.submit(() -> inserirConcorrentemente(
						prontas,
						iniciar
				)));
			}

			prontas.await();
			iniciar.countDown();

			int sucessos = 0;
			int conflitos = 0;

			for (Future<Boolean> resultado : resultados) {
				try {
					if (resultado.get()) {
						sucessos++;
					}
				} catch (ExecutionException exception) {
					conflitos++;
				}
			}

			assertEquals(1, sucessos);
			assertEquals(1, conflitos);
			assertEquals(
					1,
					jdbcTemplate.queryForObject(
							"SELECT COUNT(*) FROM agendamentos",
							Integer.class
					)
			);
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void deveFiltrarHistoricoNoPostgresqlReal() {
		inserirAgendamento(
				"2030-01-07T09:00:00-03:00",
				"2030-01-07T09:30:00-03:00",
				StatusAgendamento.CONFIRMADO
		);
		inserirAgendamento(
				"2030-01-08T09:00:00-03:00",
				"2030-01-08T09:30:00-03:00",
				StatusAgendamento.CANCELADO
		);

		var pagina = agendamentoService.listarPorProfissional(
				profissionalId,
				null,
				java.time.LocalDate.of(2030, 1, 7),
				java.time.LocalDate.of(2030, 1, 8),
				StatusAgendamento.CONFIRMADO,
				PageRequest.of(0, 20)
		);

		assertEquals(1, pagina.getTotalElements());
		assertEquals(StatusAgendamento.CONFIRMADO, pagina.getContent().get(0).getStatus());
		assertEquals(
				profissionalId,
				AgendamentoResponse.from(pagina.getContent().get(0)).profissionalId()
		);
	}

	@ParameterizedTest(name = "deve rejeitar sobreposição de {0} até {1}")
	@CsvSource({
			"2030-01-07T09:00:00-03:00, 2030-01-07T09:30:00-03:00",
			"2030-01-07T08:45:00-03:00, 2030-01-07T09:15:00-03:00",
			"2030-01-07T09:15:00-03:00, 2030-01-07T09:45:00-03:00",
			"2030-01-07T09:05:00-03:00, 2030-01-07T09:25:00-03:00",
			"2030-01-07T08:45:00-03:00, 2030-01-07T09:45:00-03:00"
	})
	void deveRejeitarTodasAsFormasDeSobreposicao(String inicio, String fim) {
		inserirAgendamento(
				"2030-01-07T09:00:00-03:00",
				"2030-01-07T09:30:00-03:00",
				StatusAgendamento.CONFIRMADO
		);

		assertThrows(
				DataIntegrityViolationException.class,
				() -> inserirAgendamento(inicio, fim, StatusAgendamento.AGENDADO)
		);
	}

	@Test
	void devePermitirAgendamentosExatamenteAdjacentes() {
		inserirAgendamento(
				"2030-01-07T09:00:00-03:00",
				"2030-01-07T09:30:00-03:00",
				StatusAgendamento.CONFIRMADO
		);
		inserirAgendamento(
				"2030-01-07T08:30:00-03:00",
				"2030-01-07T09:00:00-03:00",
				StatusAgendamento.AGENDADO
		);
		inserirAgendamento(
				"2030-01-07T09:30:00-03:00",
				"2030-01-07T10:00:00-03:00",
				StatusAgendamento.AGENDADO
		);

		assertEquals(3, quantidadeAgendamentos());
	}

	@ParameterizedTest
	@EnumSource(value = StatusAgendamento.class, names = { "CANCELADO", "CONCLUIDO" })
	void estadosEncerradosNaoDevemBloquearOHorario(StatusAgendamento status) {
		inserirAgendamento(
				"2030-01-07T09:00:00-03:00",
				"2030-01-07T09:30:00-03:00",
				status
		);
		inserirAgendamento(
				"2030-01-07T09:00:00-03:00",
				"2030-01-07T09:30:00-03:00",
				StatusAgendamento.AGENDADO
		);

		assertEquals(2, quantidadeAgendamentos());
	}

	private boolean inserirConcorrentemente(
			CountDownLatch prontas,
			CountDownLatch iniciar
	) {
		TransactionTemplate transacao = new TransactionTemplate(transactionManager);

		return Boolean.TRUE.equals(transacao.execute(status -> {
			prontas.countDown();

			try {
				iniciar.await();
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(exception);
			}

			jdbcTemplate.update(
					"INSERT INTO agendamentos "
					+ "(cliente_id, profissional_id, servico_id, barbearia_id, inicio, fim, status) "
					+ "VALUES (?, ?, ?, ?, ?, ?, 'AGENDADO')",
					clienteId,
					profissionalId,
					servicoId,
					barbeariaId,
					OffsetDateTime.parse("2030-01-07T09:00:00-03:00"),
					OffsetDateTime.parse("2030-01-07T09:30:00-03:00")
			);

			return true;
		}));
	}

	private Long inserirUsuario(String nome, String email) {
		return jdbcTemplate.queryForObject(
				"INSERT INTO usuarios (nome, email, ativo, senha_hash) "
				+ "VALUES (?, ?, TRUE, crypt('senha-teste', gen_salt('bf', 4))) "
				+ "RETURNING id",
				Long.class,
				nome,
				email
		);
	}

	private void inserirAgendamento(
			String inicio,
			String fim,
			StatusAgendamento status
	) {
		jdbcTemplate.update(
				"INSERT INTO agendamentos "
				+ "(cliente_id, profissional_id, servico_id, barbearia_id, inicio, fim, status) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?)",
				clienteId,
				profissionalId,
				servicoId,
				barbeariaId,
				OffsetDateTime.parse(inicio),
				OffsetDateTime.parse(fim),
				status.name()
		);
	}

	private int quantidadeAgendamentos() {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM agendamentos",
				Integer.class
		);
	}
}
