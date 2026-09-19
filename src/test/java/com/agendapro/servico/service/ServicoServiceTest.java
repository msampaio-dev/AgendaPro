package com.agendapro.servico.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.barbearia.exception.BarbeariaInativaException;
import com.agendapro.barbearia.exception.BarbeariaNaoEncontradaException;
import com.agendapro.barbearia.repository.BarbeariaRepository;
import com.agendapro.servico.entity.Servico;
import com.agendapro.servico.exception.ServicoJaCadastradoException;
import com.agendapro.servico.exception.ServicoNaoEncontradoException;
import com.agendapro.servico.repository.ServicoRepository;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

	private static final Long BARBEARIA_ID = 7L;

	@Mock
	private ServicoRepository servicoRepository;

	@Mock
	private BarbeariaRepository barbeariaRepository;

	@InjectMocks
	private ServicoService servicoService;

	@Test
	void deveCadastrarServicoQuandoNomeNaoExistir() {
		Barbearia barbearia = novaBarbearia();

		when(barbeariaRepository.findById(BARBEARIA_ID))
				.thenReturn(Optional.of(barbearia));
		when(servicoRepository.existsByBarbeariaIdAndNomeIgnoreCase(
				BARBEARIA_ID, "Corte de cabelo"))
				.thenReturn(false);

		when(servicoRepository.save(any(Servico.class)))
				.thenAnswer(invocacao -> invocacao.getArgument(0));

		Servico resultado = servicoService.cadastrar(
				BARBEARIA_ID,
				" Corte de cabelo ",
				"   ",
				30,
				new BigDecimal("50.00")
		);

		assertEquals("Corte de cabelo", resultado.getNome());
		assertNull(resultado.getDescricao());
		assertEquals(30, resultado.getDuracaoMinutos());
		assertEquals(new BigDecimal("50.00"), resultado.getPreco());
		assertTrue(resultado.isAtivo());
		assertSame(barbearia, resultado.getBarbearia());

		verify(servicoRepository).existsByBarbeariaIdAndNomeIgnoreCase(
				BARBEARIA_ID, "Corte de cabelo");
		verify(servicoRepository).save(any(Servico.class));
	}

	@Test
	void naoDeveCadastrarServicoQuandoNomeJaExistir() {
		when(barbeariaRepository.findById(BARBEARIA_ID))
				.thenReturn(Optional.of(novaBarbearia()));
		when(servicoRepository.existsByBarbeariaIdAndNomeIgnoreCase(
				BARBEARIA_ID, "Corte de cabelo"))
				.thenReturn(true);

		assertThrows(
				ServicoJaCadastradoException.class,
				() -> servicoService.cadastrar(
						BARBEARIA_ID,
						" Corte de cabelo ",
						"Corte tradicional",
						30,
						new BigDecimal("50.00")
				)
		);

		verify(servicoRepository).existsByBarbeariaIdAndNomeIgnoreCase(
				BARBEARIA_ID, "Corte de cabelo");
		verify(servicoRepository, never()).save(any(Servico.class));
	}

	@Test
	void deveBuscarServicoPorIdQuandoExistir() {
		Long id = 1L;
		Servico servico = novoServico("Corte de cabelo");

		when(servicoRepository.findById(id))
				.thenReturn(Optional.of(servico));

		Servico resultado = servicoService.buscarPorId(id);

		assertSame(servico, resultado);
		verify(servicoRepository).findById(id);
	}

	@Test
	void deveLancarExcecaoQuandoServicoNaoExistir() {
		Long id = 999L;

		when(servicoRepository.findById(id))
				.thenReturn(Optional.empty());

		assertThrows(
				ServicoNaoEncontradoException.class,
				() -> servicoService.buscarPorId(id)
		);

		verify(servicoRepository).findById(id);
	}

	@Test
	void deveListarServicos() {
		Servico primeiro = novoServico("Corte de cabelo");
		Servico segundo = novoServico("Barba");

		when(servicoRepository.findAllByOrderByNomeAsc())
				.thenReturn(List.of(primeiro, segundo));

		List<Servico> resultado = servicoService.listar();

		assertEquals(2, resultado.size());
		assertSame(primeiro, resultado.get(0));
		assertSame(segundo, resultado.get(1));
		verify(servicoRepository).findAllByOrderByNomeAsc();
	}

	@Test
	void deveAtualizarServicoQuandoDadosForemValidos() {
		Long id = 1L;
		Servico servico = novoServico("Corte de cabelo");

		when(servicoRepository.findById(id))
				.thenReturn(Optional.of(servico));
		when(servicoRepository.existsByBarbeariaIdAndNomeIgnoreCase(
				BARBEARIA_ID, "Corte premium"))
				.thenReturn(false);

		Servico resultado = servicoService.atualizar(
				id,
				" Corte premium ",
				" Atendimento completo ",
				45,
				new BigDecimal("75.00")
		);

		assertSame(servico, resultado);
		assertEquals("Corte premium", resultado.getNome());
		assertEquals("Atendimento completo", resultado.getDescricao());
		assertEquals(45, resultado.getDuracaoMinutos());
		assertEquals(new BigDecimal("75.00"), resultado.getPreco());
		verify(servicoRepository).existsByBarbeariaIdAndNomeIgnoreCase(
				BARBEARIA_ID, "Corte premium");
		verify(servicoRepository, never()).save(any(Servico.class));
	}

	@Test
	void naoDeveAtualizarQuandoNovoNomeJaExistir() {
		Long id = 1L;
		Servico servico = novoServico("Corte de cabelo");

		when(servicoRepository.findById(id))
				.thenReturn(Optional.of(servico));
		when(servicoRepository.existsByBarbeariaIdAndNomeIgnoreCase(
				BARBEARIA_ID, "Barba"))
				.thenReturn(true);

		assertThrows(
				ServicoJaCadastradoException.class,
				() -> servicoService.atualizar(
						id,
						"Barba",
						"Descrição nova",
						40,
						new BigDecimal("60.00")
				)
		);

		assertEquals("Corte de cabelo", servico.getNome());
		assertNull(servico.getDescricao());
		assertEquals(30, servico.getDuracaoMinutos());
		assertEquals(new BigDecimal("50.00"), servico.getPreco());
		verify(servicoRepository, never()).save(any(Servico.class));
	}

	@Test
	void deveDesativarServicoQuandoExistir() {
		Long id = 1L;
		Servico servico = novoServico("Corte de cabelo");

		when(servicoRepository.findById(id))
				.thenReturn(Optional.of(servico));

		servicoService.desativar(id);

		assertFalse(servico.isAtivo());
		verify(servicoRepository).findById(id);
		verify(servicoRepository, never()).save(any(Servico.class));
	}

	@Test
	void naoDeveCadastrarServicoQuandoBarbeariaNaoExistir() {
		when(barbeariaRepository.findById(BARBEARIA_ID))
				.thenReturn(Optional.empty());

		assertThrows(
				BarbeariaNaoEncontradaException.class,
				() -> servicoService.cadastrar(
						BARBEARIA_ID,
						"Corte de cabelo",
						null,
						30,
						new BigDecimal("50.00")
				)
		);

		verify(servicoRepository, never()).save(any(Servico.class));
	}

	@Test
	void naoDeveCadastrarServicoQuandoBarbeariaEstiverInativa() {
		Barbearia barbearia = novaBarbearia();
		barbearia.desativar();

		when(barbeariaRepository.findById(BARBEARIA_ID))
				.thenReturn(Optional.of(barbearia));

		assertThrows(
				BarbeariaInativaException.class,
				() -> servicoService.cadastrar(
						BARBEARIA_ID,
						"Corte de cabelo",
						null,
						30,
						new BigDecimal("50.00")
				)
		);

		verify(servicoRepository, never()).save(any(Servico.class));
	}

	@Test
	void deveListarServicosDaBarbearia() {
		Servico servico = novoServico("Barba");

		when(barbeariaRepository.existsById(BARBEARIA_ID)).thenReturn(true);
		when(servicoRepository.findAllByBarbeariaIdOrderByNomeAsc(BARBEARIA_ID))
				.thenReturn(List.of(servico));

		List<Servico> resultado = servicoService.listarPorBarbearia(BARBEARIA_ID);

		assertEquals(1, resultado.size());
		assertSame(servico, resultado.get(0));
	}

	private Barbearia novaBarbearia() {
		Barbearia barbearia = new Barbearia("Barbearia");
		ReflectionTestUtils.setField(barbearia, "id", BARBEARIA_ID);
		return barbearia;
	}

	private Servico novoServico(String nome) {
		return new Servico(
				nome,
				null,
				30,
				new BigDecimal("50.00"),
				novaBarbearia()
		);
	}
}
