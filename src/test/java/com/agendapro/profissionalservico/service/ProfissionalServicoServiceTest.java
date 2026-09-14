package com.agendapro.profissionalservico.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.exception.ProfissionalInativoException;
import com.agendapro.profissional.service.ProfissionalService;
import com.agendapro.profissionalservico.entity.ProfissionalServico;
import com.agendapro.profissionalservico.exception.ProfissionalServicoJaCadastradoException;
import com.agendapro.profissionalservico.exception.ProfissionalServicoNaoEncontradoException;
import com.agendapro.profissionalservico.exception.ProfissionalNaoRealizaServicoException;
import com.agendapro.profissionalservico.repository.ProfissionalServicoRepository;
import com.agendapro.servico.entity.Servico;
import com.agendapro.servico.exception.ServicoInativoException;
import com.agendapro.servico.service.ServicoService;
import com.agendapro.usuario.entity.Usuario;

@ExtendWith(MockitoExtension.class)
class ProfissionalServicoServiceTest {

	@Mock
	private ProfissionalServicoRepository profissionalServicoRepository;

	@Mock
	private ProfissionalService profissionalService;

	@Mock
	private ServicoService servicoService;

	@InjectMocks
	private ProfissionalServicoService profissionalServicoService;

	@Test
	void deveAssociarProfissionalAoServicoQuandoDadosForemValidos() {
		Long profissionalId = 1L;
		Long servicoId = 2L;
		Profissional profissional = novoProfissional();
		Servico servico = novoServico();

		when(profissionalService.buscarPorId(profissionalId))
				.thenReturn(profissional);
		when(servicoService.buscarPorId(servicoId))
				.thenReturn(servico);
		when(profissionalServicoRepository
				.findByProfissionalIdAndServicoId(profissionalId, servicoId))
				.thenReturn(Optional.empty());
		when(profissionalServicoRepository.save(any(ProfissionalServico.class)))
				.thenAnswer(invocacao -> invocacao.getArgument(0));

		ProfissionalServico resultado =
				profissionalServicoService.associar(profissionalId, servicoId);

		assertSame(profissional, resultado.getProfissional());
		assertSame(servico, resultado.getServico());
		assertTrue(resultado.isAtivo());
		verify(profissionalServicoRepository).save(resultado);
	}

	@Test
	void naoDeveAssociarQuandoProfissionalEstiverInativo() {
		Long profissionalId = 1L;
		Profissional profissional = novoProfissional();
		profissional.desativar();

		when(profissionalService.buscarPorId(profissionalId))
				.thenReturn(profissional);

		assertThrows(
				ProfissionalInativoException.class,
				() -> profissionalServicoService.associar(profissionalId, 2L)
		);

		verifyNoInteractions(servicoService, profissionalServicoRepository);
	}

	@Test
	void naoDeveAssociarQuandoServicoEstiverInativo() {
		Long profissionalId = 1L;
		Long servicoId = 2L;
		Servico servico = novoServico();
		servico.desativar();

		when(profissionalService.buscarPorId(profissionalId))
				.thenReturn(novoProfissional());
		when(servicoService.buscarPorId(servicoId))
				.thenReturn(servico);

		assertThrows(
				ServicoInativoException.class,
				() -> profissionalServicoService.associar(profissionalId, servicoId)
		);

		verify(profissionalServicoRepository, never())
				.save(any(ProfissionalServico.class));
	}

	@Test
	void naoDeveAssociarQuandoCombinacaoJaExistir() {
		Long profissionalId = 1L;
		Long servicoId = 2L;
		ProfissionalServico associacaoExistente =
				new ProfissionalServico(novoProfissional(), novoServico());

		when(profissionalService.buscarPorId(profissionalId))
				.thenReturn(novoProfissional());
		when(servicoService.buscarPorId(servicoId))
				.thenReturn(novoServico());
		when(profissionalServicoRepository
				.findByProfissionalIdAndServicoId(profissionalId, servicoId))
				.thenReturn(Optional.of(associacaoExistente));

		assertThrows(
				ProfissionalServicoJaCadastradoException.class,
				() -> profissionalServicoService.associar(profissionalId, servicoId)
		);

		verify(profissionalServicoRepository, never())
				.save(any(ProfissionalServico.class));
	}

	@Test
	void deveReativarAssociacaoQuandoElaExistirInativa() {
		Long profissionalId = 1L;
		Long servicoId = 2L;
		ProfissionalServico associacaoExistente =
				new ProfissionalServico(novoProfissional(), novoServico());
		associacaoExistente.desativar();

		when(profissionalService.buscarPorId(profissionalId))
				.thenReturn(novoProfissional());
		when(servicoService.buscarPorId(servicoId))
				.thenReturn(novoServico());
		when(profissionalServicoRepository
				.findByProfissionalIdAndServicoId(profissionalId, servicoId))
				.thenReturn(Optional.of(associacaoExistente));

		ProfissionalServico resultado =
				profissionalServicoService.associar(profissionalId, servicoId);

		assertSame(associacaoExistente, resultado);
		assertTrue(resultado.isAtivo());
		verify(profissionalServicoRepository, never())
				.save(any(ProfissionalServico.class));
	}

	@Test
	void deveListarAssociacoesAtivasDoProfissional() {
		Long profissionalId = 1L;
		Profissional profissional = novoProfissional();
		ProfissionalServico primeira =
				new ProfissionalServico(profissional, novoServico());
		ProfissionalServico segunda =
				new ProfissionalServico(profissional, novoServico());

		when(profissionalService.buscarPorId(profissionalId))
				.thenReturn(profissional);
		when(profissionalServicoRepository
				.findAllByProfissionalIdAndAtivoTrue(profissionalId))
				.thenReturn(List.of(primeira, segunda));

		List<ProfissionalServico> resultado = profissionalServicoService
				.listarAtivosPorProfissional(profissionalId);

		assertSame(primeira, resultado.get(0));
		assertSame(segunda, resultado.get(1));
		verify(profissionalService).buscarPorId(profissionalId);
		verify(profissionalServicoRepository)
				.findAllByProfissionalIdAndAtivoTrue(profissionalId);
	}

	@Test
	void deveDesativarAssociacaoQuandoExistir() {
		Long id = 1L;
		ProfissionalServico profissionalServico =
				new ProfissionalServico(novoProfissional(), novoServico());

		when(profissionalServicoRepository.findById(id))
				.thenReturn(Optional.of(profissionalServico));

		profissionalServicoService.desativar(id);

		assertFalse(profissionalServico.isAtivo());
		verify(profissionalServicoRepository, never())
				.save(any(ProfissionalServico.class));
	}

	@Test
	void deveLancarExcecaoAoDesativarAssociacaoInexistente() {
		Long id = 999L;

		when(profissionalServicoRepository.findById(id))
				.thenReturn(Optional.empty());

		assertThrows(
				ProfissionalServicoNaoEncontradoException.class,
				() -> profissionalServicoService.desativar(id)
		);
	}

	@Test
	void deveRejeitarQuandoAssociacaoAtivaNaoExistir() {
		when(profissionalServicoRepository
				.existsByProfissionalIdAndServicoIdAndAtivoTrue(1L, 2L))
				.thenReturn(false);

		assertThrows(
				ProfissionalNaoRealizaServicoException.class,
				() -> profissionalServicoService.validarAssociacaoAtiva(1L, 2L)
		);
	}

	private Profissional novoProfissional() {
		return new Profissional(
				new Usuario("Marcelo", "marcelo@agendapro.com")
		);
	}

	private Servico novoServico() {
		return new Servico(
				"Corte de cabelo",
				null,
				30,
				new BigDecimal("50.00")
		);
	}
}
