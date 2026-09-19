package com.agendapro.shared.startup;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.agendapro.shared.PostgresIntegrationTest;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "app.aquecimento.enabled=true"
)
class AquecimentoAplicacaoIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private AquecimentoAplicacao aquecimento;

	@LocalServerPort
	private int porta;

	@Test
	void deveAquecerSozinhoDepoisDoArranque() throws InterruptedException {
		// O aquecimento roda em outra thread para nao segurar o arranque, entao o
		// teste espera por ele em vez de assumir que ja terminou.
		Instant limite = Instant.now().plusSeconds(30);

		while (!aquecimento.concluido() && Instant.now().isBefore(limite)) {
			Thread.sleep(Duration.ofMillis(100));
		}

		assertTrue(aquecimento.concluido(), "o aquecimento deveria ter concluido apos o arranque");
		assertTrue(porta > 0, "o servidor deveria ter subido numa porta real");
	}
}
