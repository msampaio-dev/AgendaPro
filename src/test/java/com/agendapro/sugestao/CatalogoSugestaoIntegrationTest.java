package com.agendapro.sugestao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.agendapro.shared.PostgresIntegrationTest;
import com.agendapro.sugestao.ia.CatalogoSugestao;
import com.agendapro.sugestao.service.CatalogoSugestaoService;

@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/devdata")
class CatalogoSugestaoIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private CatalogoSugestaoService catalogoService;

	@Test
	void deveLimitarServicosPorBarbeariaMantendoOsMaisAntigos() {
		Long ipanema = jdbc.queryForObject(
				"SELECT id FROM barbearias WHERE nome = 'Barbershopping Ipanema'", Long.class);
		// Nomes que vem antes em ordem alfabetica, como faria quem quisesse
		// empurrar os servicos reais para fora do prompt.
		for (int i = 0; i < 40; i++) {
			jdbc.update("INSERT INTO servicos (barbearia_id, nome, duracao_minutos, preco, ativo) "
					+ "VALUES (?, ?, 30, 10.00, TRUE)", ipanema, "AAA inflado %02d".formatted(i));
		}

		CatalogoSugestao catalogo = catalogoService.montar(LocalDate.of(2026, 10, 7));

		CatalogoSugestao.Barbearia unidade = catalogo.barbearias().stream()
				.filter(barbearia -> barbearia.id().equals(ipanema))
				.findFirst()
				.orElseThrow();
		assertEquals(30, unidade.servicos().size());
		assertTrue(unidade.servicos().stream().anyMatch(servico -> servico.nome().equals("Barba")),
				"os servicos originais continuam no catalogo");

		// Associacao com um servico que ficou fora do corte nao vai para o prompt.
		Long ultimoInflado = jdbc.queryForObject(
				"SELECT MAX(id) FROM servicos WHERE barbearia_id = ?", Long.class, ipanema);
		Long profissionalId = unidade.profissionais().get(0).id();
		jdbc.update("INSERT INTO profissionais_servicos (profissional_id, servico_id, ativo) VALUES (?, ?, TRUE)",
				profissionalId, ultimoInflado);
		CatalogoSugestao.Profissional profissional = catalogoService.montar(LocalDate.of(2026, 10, 7))
				.barbearias().stream()
				.filter(barbearia -> barbearia.id().equals(ipanema))
				.findFirst().orElseThrow()
				.profissionais().stream()
				.filter(item -> item.id().equals(profissionalId))
				.findFirst().orElseThrow();
		assertTrue(!profissional.servicoIds().contains(ultimoInflado));
		assertTrue(unidade.servicos().stream()
				.allMatch(servico -> !servico.id().equals(ultimoInflado)));
	}
}
