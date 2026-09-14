package com.agendapro.auth.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.agendapro.auth.exception.CredenciaisInvalidasException;
import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

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

		verify(passwordEncoder, never()).matches(org.mockito.ArgumentMatchers.any(),
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

		verify(passwordEncoder, never()).matches(org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
	}
}
