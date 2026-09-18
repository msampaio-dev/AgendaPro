package com.agendapro.auth.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.repository.ProfissionalRepository;
import com.agendapro.auth.exception.CredenciaisInvalidasException;
import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private ProfissionalRepository profissionalRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private LoginRateLimiter rateLimiter;

	@InjectMocks
	private AuthService authService;

	@Test
	void deveAutenticarComCredenciaisCorretas() {
		Usuario usuario = new Usuario("Marcelo", "marcelo@agendapro.com", "hash-bcrypt");
		when(usuarioRepository.findByEmailIgnoreCase("marcelo@agendapro.com"))
				.thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("senha-segura", "hash-bcrypt")).thenReturn(true);

		Usuario resultado = authService.autenticar(" MARCELO@AGENDAPRO.COM ", "senha-segura");

		assertSame(usuario, resultado);
		verify(passwordEncoder).matches("senha-segura", "hash-bcrypt");
	}

	@Test
	void naoDeveAutenticarComSenhaIncorreta() {
		Usuario usuario = new Usuario("Marcelo", "marcelo@agendapro.com", "hash-bcrypt");
		when(usuarioRepository.findByEmailIgnoreCase("marcelo@agendapro.com"))
				.thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("senha-errada", "hash-bcrypt")).thenReturn(false);

		assertThrows(CredenciaisInvalidasException.class,
				() -> authService.autenticar("marcelo@agendapro.com", "senha-errada"));
	}

	@Test
	void naoDeveAutenticarQuandoEmailNaoExiste() {
		when(usuarioRepository.findByEmailIgnoreCase("ausente@agendapro.com"))
				.thenReturn(Optional.empty());

		assertThrows(CredenciaisInvalidasException.class,
				() -> authService.autenticar("ausente@agendapro.com", "senha-segura"));

		// matches deve rodar mesmo sem usuário, pra não vazar por timing se o e-mail existe
		verify(passwordEncoder).matches(org.mockito.ArgumentMatchers.eq("senha-segura"),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void naoDeveAutenticarUsuarioInativo() {
		Usuario usuario = new Usuario("Marcelo", "marcelo@agendapro.com", "hash-bcrypt");
		usuario.desativar();
		when(usuarioRepository.findByEmailIgnoreCase("marcelo@agendapro.com"))
				.thenReturn(Optional.of(usuario));

		assertThrows(CredenciaisInvalidasException.class,
				() -> authService.autenticar("marcelo@agendapro.com", "senha-segura"));

		verify(passwordEncoder).matches("senha-segura", "hash-bcrypt");
	}

	@Test
	void deveBuscarSessaoDeClienteSemProfissional() {
		Usuario usuario = new Usuario("Marcelo", "marcelo@agendapro.com", "hash-bcrypt");
		ReflectionTestUtils.setField(usuario, "id", 1L);
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
		when(profissionalRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

		var sessao = authService.buscarSessao(1L);

		assertEquals(1L, sessao.id());
		assertEquals("Marcelo", sessao.nome());
		assertNull(sessao.profissionalId());
	}

	@Test
	void deveBuscarSessaoComIdDoProfissional() {
		Usuario usuario = new Usuario("Marcelo", "marcelo@agendapro.com", "hash-bcrypt");
		ReflectionTestUtils.setField(usuario, "id", 1L);
		Profissional profissional = new Profissional(usuario);
		ReflectionTestUtils.setField(profissional, "id", 7L);
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
		when(profissionalRepository.findByUsuarioId(1L)).thenReturn(Optional.of(profissional));

		var sessao = authService.buscarSessao(1L);

		assertEquals(7L, sessao.profissionalId());
		assertEquals("America/Sao_Paulo", sessao.fusoHorario());
	}
}
