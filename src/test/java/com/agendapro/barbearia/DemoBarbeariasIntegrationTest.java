package com.agendapro.barbearia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
