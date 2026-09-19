package com.agendapro.barbearia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.agendapro.shared.PostgresIntegrationTest;

@SpringBootTest(properties =
		"spring.flyway.locations=classpath:db/migration,classpath:db/devdata")
class DemoBarbeariasIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void deveCriarBarbeariasEquipesServicosEExpedienteDeDemonstracao() {
		assertEquals(3L, contar("""
				SELECT COUNT(*) FROM barbearias
				WHERE ativo = TRUE AND nome IN (
					'Barbershopping Ipanema',
					'Barbearia da Comunidade',
					'Barbershop Morumbi'
				)
				"""));
		assertEquals(6L, contar("""
				SELECT COUNT(*) FROM profissionais p
				JOIN usuarios u ON u.id = p.usuario_id
				WHERE u.email LIKE '%@demo.agendapro.local'
				"""));
		assertEquals(2L, profissionaisDa("Barbershopping Ipanema"));
		assertEquals(1L, profissionaisDa("Barbearia da Comunidade"));
		assertEquals(3L, profissionaisDa("Barbershop Morumbi"));
		assertEquals(18L, contar("""
				SELECT COUNT(*) FROM profissionais_servicos ps
				JOIN profissionais p ON p.id = ps.profissional_id
				JOIN usuarios u ON u.id = p.usuario_id
				WHERE u.email LIKE '%@demo.agendapro.local'
				"""));
		assertEquals(72L, contar("""
				SELECT COUNT(*) FROM horarios_atendimento h
				JOIN profissionais p ON p.id = h.profissional_id
				JOIN usuarios u ON u.id = p.usuario_id
				WHERE u.email LIKE '%@demo.agendapro.local'
				"""));
		String senhaHash = jdbc.queryForObject("""
				SELECT senha_hash FROM usuarios
				WHERE email = 'joao.gabriel@demo.agendapro.local'
				""", String.class);
		assertTrue(new BCryptPasswordEncoder().matches("Demo123!", senhaHash));
	}

	@Test
	void cadaBarbeariaDeveTerPrecoProprioComBarbaComoReferencia() {
		// O catalogo deixou de ser global: se os tres precos de um mesmo servico
		// voltarem a ser iguais, a demonstracao para de mostrar isso na tela.
		assertEquals(new BigDecimal("50.00"), preco("Barbershopping Ipanema", "Corte de cabelo Social"));
		assertEquals(new BigDecimal("35.00"), preco("Barbearia da Comunidade", "Corte de cabelo Social"));
		assertEquals(new BigDecimal("45.00"), preco("Barbershop Morumbi", "Corte de cabelo Social"));

		assertEquals(3L, contar("""
				SELECT COUNT(*) FROM servicos s
				JOIN barbearias b ON b.id = s.barbearia_id
				WHERE s.nome = 'Barba' AND s.preco = 20.00
				  AND b.nome IN (
					'Barbershopping Ipanema',
					'Barbearia da Comunidade',
					'Barbershop Morumbi'
				  )
				"""));

		assertEquals(0L, contar("""
				SELECT COUNT(*) FROM servicos
				WHERE nome LIKE 'Corte%' AND (preco < 35.00 OR preco > 60.00)
				"""));
	}

	private BigDecimal preco(String barbearia, String servico) {
		return jdbc.queryForObject("""
				SELECT s.preco FROM servicos s
				JOIN barbearias b ON b.id = s.barbearia_id
				WHERE b.nome = ? AND s.nome = ?
				""", BigDecimal.class, barbearia, servico);
	}

	private long profissionaisDa(String nome) {
		Long valor = jdbc.queryForObject("""
				SELECT COUNT(*) FROM profissionais p
				JOIN barbearias b ON b.id = p.barbearia_id
				WHERE b.nome = ?
				""", Long.class, nome);
		return valor == null ? 0 : valor;
	}

	private long contar(String sql) {
		Long valor = jdbc.queryForObject(sql, Long.class);
		return valor == null ? 0 : valor;
	}
}
