package com.agendapro.usuario.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.agendapro.shared.PostgresIntegrationTest;
import com.agendapro.usuario.entity.PerfilUsuario;
import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.repository.UsuarioRepository;

@SpringBootTest(properties = {
		"app.bootstrap.admin.enabled=true",
		"app.bootstrap.admin.nome=Administrador Teste",
		"app.bootstrap.admin.email=admin.teste@agendapro.com",
		"app.bootstrap.admin.senha=senha-segura"
})
class AdminBootstrapIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void deveCriarPrimeiroAdminComSenhaProtegida() {
		Usuario admin = usuarioRepository.findByEmailIgnoreCase("admin.teste@agendapro.com")
				.orElseThrow();

		assertEquals("Administrador Teste", admin.getNome());
		assertTrue(admin.getPerfis().contains(PerfilUsuario.CLIENTE));
		assertTrue(admin.getPerfis().contains(PerfilUsuario.ADMIN));
		assertTrue(passwordEncoder.matches("senha-segura", admin.getSenhaHash()));
		assertEquals(1, usuarioRepository.count());
	}
}
