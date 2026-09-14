package com.agendapro.usuario.bootstrap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agendapro.usuario.entity.PerfilUsuario;
import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.repository.UsuarioRepository;
import com.agendapro.usuario.service.UsuarioService;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class PrimeiroAdminServiceTest {

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private UsuarioService usuarioService;

	private PrimeiroAdminService primeiroAdminService;

	@BeforeEach
	void configurar() {
		Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
		primeiroAdminService = new PrimeiroAdminService(usuarioRepository, usuarioService, validator);
	}

	@Test
	void deveCriarPrimeiroAdminQuandoAindaNaoExistir() {
		AdminBootstrapProperties properties = dadosValidos();
		Usuario usuario = new Usuario("Administrador", "admin@agendapro.com", "hash");

		when(usuarioRepository.existsByPerfil(PerfilUsuario.ADMIN)).thenReturn(false);
		when(usuarioRepository.existsByEmailIgnoreCase("admin@agendapro.com")).thenReturn(false);
		when(usuarioService.cadastrar("Administrador", "admin@agendapro.com", "senha-segura"))
				.thenReturn(usuario);

		boolean criado = primeiroAdminService.criarSeNecessario(properties);

		assertTrue(criado);
		assertTrue(usuario.getPerfis().contains(PerfilUsuario.ADMIN));
	}

	@Test
	void naoDeveCriarOutroAdminQuandoJaExistirUm() {
		when(usuarioRepository.existsByPerfil(PerfilUsuario.ADMIN)).thenReturn(true);

		boolean criado = primeiroAdminService.criarSeNecessario(dadosValidos());

		assertFalse(criado);
		verifyNoInteractions(usuarioService);
		verify(usuarioRepository, never()).existsByEmailIgnoreCase("admin@agendapro.com");
	}

	@Test
	void deveFalharQuandoEmailDoBootstrapJaPertencerAOutroUsuario() {
		when(usuarioRepository.existsByPerfil(PerfilUsuario.ADMIN)).thenReturn(false);
		when(usuarioRepository.existsByEmailIgnoreCase("admin@agendapro.com")).thenReturn(true);

		assertThrows(
				IllegalStateException.class,
				() -> primeiroAdminService.criarSeNecessario(dadosValidos())
		);

		verifyNoInteractions(usuarioService);
	}

	@Test
	void deveFalharQuandoConfiguracaoDoBootstrapForInvalida() {
		AdminBootstrapProperties properties = new AdminBootstrapProperties(
				true,
				"",
				"email-invalido",
				"123"
		);
		when(usuarioRepository.existsByPerfil(PerfilUsuario.ADMIN)).thenReturn(false);

		assertThrows(
				IllegalStateException.class,
				() -> primeiroAdminService.criarSeNecessario(properties)
		);

		verify(usuarioRepository, never()).existsByEmailIgnoreCase("email-invalido");
		verifyNoInteractions(usuarioService);
	}

	private AdminBootstrapProperties dadosValidos() {
		return new AdminBootstrapProperties(
				true,
				"Administrador",
				"admin@agendapro.com",
				"senha-segura"
		);
	}
}
