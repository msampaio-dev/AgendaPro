package com.agendapro.profissional.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.exception.ProfissionalJaCadastradoException;
import com.agendapro.profissional.exception.ProfissionalNaoEncontradoException;
import com.agendapro.profissional.repository.ProfissionalRepository;
import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.entity.PerfilUsuario;
import com.agendapro.usuario.exception.UsuarioInativoException;
import com.agendapro.usuario.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class ProfissionalServiceTest {

	@Mock
	private ProfissionalRepository profissionalRepository;

	@Mock
	private UsuarioService usuarioService;

	@InjectMocks
	private ProfissionalService profissionalService;

	@Test
	void deveCadastrarProfissionalParaUsuarioAtivo() {
		Long usuarioId = 1L;
		Usuario usuario = new Usuario("Marcelo", "marcelo@email.com");

		when(usuarioService.buscarPorId(usuarioId))
				.thenReturn(usuario);

		when(profissionalRepository.existsByUsuarioId(usuarioId))
				.thenReturn(false);

		when(profissionalRepository.save(any(Profissional.class)))
				.thenAnswer(invocacao -> invocacao.getArgument(0));

		Profissional resultado = profissionalService.cadastrar(usuarioId);

		assertSame(usuario, resultado.getUsuario());
		assertTrue(resultado.isAtivo());
		assertTrue(usuario.getPerfis().contains(PerfilUsuario.PROFISSIONAL));

		verify(usuarioService).buscarPorId(usuarioId);
		verify(profissionalRepository).existsByUsuarioId(usuarioId);
		verify(profissionalRepository).save(any(Profissional.class));
	}

	@Test
	void naoDeveCadastrarProfissionalParaUsuarioInativo() {
		Long usuarioId = 1L;
		Usuario usuario = new Usuario("Marcelo", "marcelo@email.com");
		usuario.desativar();

		when(usuarioService.buscarPorId(usuarioId))
				.thenReturn(usuario);

		assertThrows(
				UsuarioInativoException.class,
				() -> profissionalService.cadastrar(usuarioId)
		);

		verify(profissionalRepository, never()).existsByUsuarioId(usuarioId);
		verify(profissionalRepository, never()).save(any(Profissional.class));
	}

	@Test
	void naoDeveCadastrarQuandoUsuarioJaForProfissional() {
		Long usuarioId = 1L;
		Usuario usuario = new Usuario("Marcelo", "marcelo@email.com");

		when(usuarioService.buscarPorId(usuarioId))
				.thenReturn(usuario);

		when(profissionalRepository.existsByUsuarioId(usuarioId))
				.thenReturn(true);

		assertThrows(
				ProfissionalJaCadastradoException.class,
				() -> profissionalService.cadastrar(usuarioId)
		);

		verify(usuarioService).buscarPorId(usuarioId);
		verify(profissionalRepository).existsByUsuarioId(usuarioId);
		verify(profissionalRepository, never()).save(any(Profissional.class));
	}

	@Test
	void deveBuscarProfissionalPorIdQuandoExistir() {
		Long id = 1L;
		Usuario usuario = new Usuario("Marcelo", "marcelo@email.com");
		Profissional profissional = new Profissional(usuario);

		when(profissionalRepository.findById(id))
				.thenReturn(Optional.of(profissional));

		Profissional resultado = profissionalService.buscarPorId(id);

		assertSame(profissional, resultado);
		verify(profissionalRepository).findById(id);
	}

	@Test
	void deveLancarExcecaoQuandoProfissionalNaoExistir() {
		Long id = 999L;

		when(profissionalRepository.findById(id))
				.thenReturn(Optional.empty());

		assertThrows(
				ProfissionalNaoEncontradoException.class,
				() -> profissionalService.buscarPorId(id)
		);

		verify(profissionalRepository).findById(id);
	}

	@Test
	void deveListarProfissionais() {
		Profissional primeiro = new Profissional(
				new Usuario("Marcelo", "marcelo@email.com")
		);
		Profissional segundo = new Profissional(
				new Usuario("Ana", "ana@email.com")
		);

		when(profissionalRepository.findAll())
				.thenReturn(List.of(primeiro, segundo));

		List<Profissional> resultado = profissionalService.listar();

		assertEquals(2, resultado.size());
		assertSame(primeiro, resultado.get(0));
		assertSame(segundo, resultado.get(1));
		verify(profissionalRepository).findAll();
	}

	@Test
	void deveDesativarProfissionalExistente() {
		Long id = 1L;
		Profissional profissional = new Profissional(
				new Usuario("Marcelo", "marcelo@email.com")
		);

		when(profissionalRepository.findById(id))
				.thenReturn(Optional.of(profissional));

		profissionalService.desativar(id);

		assertFalse(profissional.isAtivo());
		assertFalse(profissional.getUsuario().getPerfis().contains(PerfilUsuario.PROFISSIONAL));
		verify(profissionalRepository).findById(id);
		verify(profissionalRepository, never()).delete(any(Profissional.class));
	}
}
