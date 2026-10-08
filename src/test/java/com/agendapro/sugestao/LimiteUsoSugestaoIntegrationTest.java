package com.agendapro.sugestao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.agendapro.shared.PostgresIntegrationTest;
import com.agendapro.sugestao.exception.LimiteSugestoesAtingidoException;
import com.agendapro.sugestao.service.LimiteUsoSugestao;

@SpringBootTest(properties = {
		"app.ia.sugestao.limite-por-usuario=3",
		"app.ia.sugestao.limite-global=5"
})
class LimiteUsoSugestaoIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private LimiteUsoSugestao limite;

	@BeforeEach
	void limpar() {
		jdbcTemplate.execute("TRUNCATE TABLE usos_sugestao_agendamento, usuarios RESTART IDENTITY CASCADE");
	}

	@Test
	void deveContarUsosEBloquearDepoisDoLimiteDoUsuario() {
		Long usuarioId = inserirUsuario("cliente@teste.com");

		assertEquals(2, limite.registrarUso(usuarioId));
		assertEquals(1, limite.registrarUso(usuarioId));
		assertEquals(0, limite.registrarUso(usuarioId));
		assertThrows(LimiteSugestoesAtingidoException.class, () -> limite.registrarUso(usuarioId));

		// A tentativa recusada nao fica contada: o rollback desfaz o incremento.
		assertEquals(3, usosRegistrados(usuarioId));
	}

	@Test
	void deveBloquearQuandoOTetoGlobalDoDiaEstoura() {
		Long primeiro = inserirUsuario("primeiro@teste.com");
		Long segundo = inserirUsuario("segundo@teste.com");

		limite.registrarUso(primeiro);
		limite.registrarUso(primeiro);
		limite.registrarUso(primeiro);
		limite.registrarUso(segundo);
		limite.registrarUso(segundo);

		assertThrows(LimiteSugestoesAtingidoException.class, () -> limite.registrarUso(segundo));
	}

	@Test
	void deveContarRequisicoesSimultaneasDoMesmoUsuarioSemPerderIncremento() throws Exception {
		Long usuarioId = inserirUsuario("concorrente@teste.com");
		int requisicoes = 6;
		CountDownLatch largada = new CountDownLatch(1);
		List<Future<Boolean>> resultados = new ArrayList<>();

		try (ExecutorService executor = Executors.newFixedThreadPool(requisicoes)) {
			for (int i = 0; i < requisicoes; i++) {
				resultados.add(executor.submit(() -> {
					largada.await();
					try {
						limite.registrarUso(usuarioId);
						return true;
					} catch (LimiteSugestoesAtingidoException exception) {
						return false;
					}
				}));
			}
			largada.countDown();

			long aceitas = 0;
			for (Future<Boolean> resultado : resultados) {
				if (resultado.get()) aceitas++;
			}
			assertEquals(3, aceitas);
		}
		assertEquals(3, usosRegistrados(usuarioId));
	}

	private int usosRegistrados(Long usuarioId) {
		return jdbcTemplate.queryForObject(
				"SELECT quantidade FROM usos_sugestao_agendamento WHERE usuario_id = ?",
				Integer.class,
				usuarioId
		);
	}

	private Long inserirUsuario(String email) {
		return jdbcTemplate.queryForObject(
				"INSERT INTO usuarios (nome, email, ativo, senha_hash) "
				+ "VALUES ('Cliente', ?, TRUE, crypt('senha-teste', gen_salt('bf', 4))) "
				+ "RETURNING id",
				Long.class,
				email
		);
	}
}
