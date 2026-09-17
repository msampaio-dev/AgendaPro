package com.agendapro.usuario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.entity.PerfilUsuario;
import com.agendapro.usuario.exception.EmailJaCadastradoException;
import com.agendapro.usuario.exception.UsuarioNaoEncontradoException;
import com.agendapro.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UsuarioService usuarioService;

	@Test
	void naoDeveCadastrarUsuarioQuandoEmailJaExiste() {
		String emailInformado = " Marcelo@Email.com ";
		String emailNormalizado = "marcelo@email.com";

		when(usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)).thenReturn(true);

		assertThrows(EmailJaCadastradoException.class,
				() -> usuarioService.cadastrar("Marcelo", emailInformado, "senha-segura"));

		verify(usuarioRepository, never()).save(any());
		verify(passwordEncoder, never()).encode(any());
	}

	@Test
	void deveCadastrarUsuarioQuandoEmailNaoExiste() {
		String emailInformado = " Marcelo@Email.com ";
		String emailNormalizado = "marcelo@email.com";

		when(usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)).thenReturn(false);
		when(passwordEncoder.encode("senha-segura")).thenReturn("hash-bcrypt");

		when(usuarioRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

		var usuarioCadastrado = usuarioService.cadastrar(" Marcelo ", emailInformado, "senha-segura");

		assertEquals("Marcelo", usuarioCadastrado.getNome());
		assertEquals(emailNormalizado, usuarioCadastrado.getEmail());
		assertEquals("hash-bcrypt", usuarioCadastrado.getSenhaHash());

		verify(passwordEncoder).encode("senha-segura");
		verify(usuarioRepository).save(any());
	}

	@Test
	void deveBuscarUsuarioPorIdQuandoExistir() {
		Long id = 1L;
		Usuario usuario = new Usuario("Marcelo", "marcelo@email.com");

		when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

		Usuario resultado = usuarioService.buscarPorId(id);

		assertSame(usuario, resultado);
		verify(usuarioRepository).findById(id);
	}

	@Test
	void deveLancarExcecaoQuandoUsuarioNaoExistir() {
		Long id = 999L;

		when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

		assertThrows(UsuarioNaoEncontradoException.class, () -> usuarioService.buscarPorId(id));

		verify(usuarioRepository).findById(id);
	}

	@Test
	void deveListarUsuarios() {
		Usuario primeiroUsuario = new Usuario("Marcelo", "marcelo@email.com");

		Usuario segundoUsuario = new Usuario("Ana", "ana@email.com");

		when(usuarioRepository.findAll()).thenReturn(List.of(primeiroUsuario, segundoUsuario));

		List<Usuario> resultado = usuarioService.listar();

		assertEquals(2, resultado.size());
		assertSame(primeiroUsuario, resultado.get(0));
		assertSame(segundoUsuario, resultado.get(1));

		verify(usuarioRepository).findAll();
	}

	@Test
	void deveAtualizarUsuarioQuandoDadosForemValidos() {
		Long id = 1L;
		Usuario usuario = new Usuario("Marcelo", "marcelo@email.com");

		when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

		when(usuarioRepository.existsByEmailIgnoreCase("novo@email.com")).thenReturn(false);

		Usuario resultado = usuarioService.atualizar(id, " Marcelo Silva ", " NOVO@EMAIL.COM ");

		assertSame(usuario, resultado);
		assertEquals("Marcelo Silva", resultado.getNome());
		assertEquals("novo@email.com", resultado.getEmail());

		verify(usuarioRepository).findById(id);
		verify(usuarioRepository).existsByEmailIgnoreCase("novo@email.com");
	}

	@Test
	void naoDeveAtualizarQuandoNovoEmailJaExistir() {
		Long id = 1L;
		Usuario usuario = new Usuario("Marcelo", "marcelo@email.com");

		when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

		when(usuarioRepository.existsByEmailIgnoreCase("ana@email.com")).thenReturn(true);

		assertThrows(EmailJaCadastradoException.class, () -> usuarioService.atualizar(id, "Marcelo", "ana@email.com"));

		assertEquals("Marcelo", usuario.getNome());
		assertEquals("marcelo@email.com", usuario.getEmail());

		verify(usuarioRepository).findById(id);
		verify(usuarioRepository).existsByEmailIgnoreCase("ana@email.com");
	}

	@Test
	void deveDesativarUsuarioExistente() {
		Long id = 1L;
		Usuario usuario = new Usuario("Marcelo", "marcelo@email.com");

		when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

		usuarioService.desativar(id);

		assertFalse(usuario.isAtivo());

		verify(usuarioRepository).findById(id);
		verify(usuarioRepository, never()).delete(any(Usuario.class));
	}

	@Test
	void deveReativarUsuarioExistente() {
		Long id = 1L;
		Usuario usuario = new Usuario("Marcelo", "marcelo@email.com");
		usuario.desativar();
		when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

		Usuario resultado = usuarioService.reativar(id);

		assertTrue(resultado.isAtivo());
		verify(usuarioRepository).findById(id);
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void deveListarUsuariosParaAdministracaoComFiltros() {
		Usuario usuario = new Usuario("Marcelo", "marcelo@email.com");
		PageRequest pagina = PageRequest.of(0, 20, Sort.by("nome"));
		when(usuarioRepository.findAll(
				org.mockito.ArgumentMatchers.<Specification<Usuario>>any(),
				org.mockito.ArgumentMatchers.eq(pagina)
		)).thenReturn(new PageImpl<>(List.of(usuario), pagina, 1));

		var resultado = usuarioService.listarParaAdministracao(
				"marcelo",
				true,
				PerfilUsuario.CLIENTE,
				pagina
		);

		assertEquals(1, resultado.getTotalElements());
		assertSame(usuario, resultado.getContent().get(0));
	}
}
