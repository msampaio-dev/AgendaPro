package com.agendapro.disponibilidade.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agendapro.disponibilidade.entity.ExcecaoDisponibilidade;
import com.agendapro.disponibilidade.entity.TipoExcecaoDisponibilidade;
import com.agendapro.disponibilidade.exception.ExcecaoDisponibilidadeInvalidaException;
import com.agendapro.disponibilidade.exception.ExcecaoDisponibilidadeJaCadastradaException;
import com.agendapro.disponibilidade.repository.ExcecaoDisponibilidadeRepository;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.service.ProfissionalService;
import com.agendapro.usuario.entity.Usuario;

@ExtendWith(MockitoExtension.class)
class ExcecaoDisponibilidadeServiceTest {

	private static final LocalDate DATA = LocalDate.of(2030, 1, 7);

	@Mock
	private ExcecaoDisponibilidadeRepository excecaoRepository;

	@Mock
	private ProfissionalService profissionalService;

	@InjectMocks
	private ExcecaoDisponibilidadeService excecaoService;

	@Test
	void deveCadastrarBloqueioDeDiaInteiro() {
		Profissional profissional = novoProfissional();

		when(profissionalService.buscarPorId(1L)).thenReturn(profissional);
		when(excecaoRepository
				.existsByProfissionalIdAndDataAndTipoAndHorarioInicioAndHorarioFimAndAtivoTrue(
						1L,
						DATA,
						TipoExcecaoDisponibilidade.BLOQUEIO,
						null,
						null
				))
				.thenReturn(false);
		when(excecaoRepository.save(any(ExcecaoDisponibilidade.class)))
				.thenAnswer(invocacao -> invocacao.getArgument(0));

		ExcecaoDisponibilidade resultado = excecaoService.cadastrar(
				1L,
				DATA,
				TipoExcecaoDisponibilidade.BLOQUEIO,
				null,
				null
		);

		assertSame(profissional, resultado.getProfissional());
		assertTrue(resultado.isDiaInteiro());
		assertNull(resultado.getHorarioInicio());
		assertTrue(resultado.isAtivo());
	}

	@Test
	void deveCadastrarDisponibilidadeExtraComIntervalo() {
		when(profissionalService.buscarPorId(1L))
				.thenReturn(novoProfissional());
		when(excecaoRepository
				.existsByProfissionalIdAndDataAndTipoAndHorarioInicioAndHorarioFimAndAtivoTrue(
						1L,
						DATA,
						TipoExcecaoDisponibilidade.DISPONIBILIDADE_EXTRA,
						LocalTime.of(18, 0),
						LocalTime.of(20, 0)
				))
				.thenReturn(false);
		when(excecaoRepository.save(any(ExcecaoDisponibilidade.class)))
				.thenAnswer(invocacao -> invocacao.getArgument(0));

		ExcecaoDisponibilidade resultado = excecaoService.cadastrar(
				1L,
				DATA,
				TipoExcecaoDisponibilidade.DISPONIBILIDADE_EXTRA,
				LocalTime.of(18, 0),
				LocalTime.of(20, 0)
		);

		assertFalse(resultado.isDiaInteiro());
		assertSame(
				TipoExcecaoDisponibilidade.DISPONIBILIDADE_EXTRA,
				resultado.getTipo()
		);
	}

	@Test
	void naoDeveCadastrarDisponibilidadeExtraSemIntervalo() {
		assertThrows(
				ExcecaoDisponibilidadeInvalidaException.class,
				() -> excecaoService.cadastrar(
						1L,
						DATA,
						TipoExcecaoDisponibilidade.DISPONIBILIDADE_EXTRA,
						null,
						null
				)
		);

		verifyNoInteractions(profissionalService, excecaoRepository);
	}

	@Test
	void naoDeveCadastrarQuandoApenasUmHorarioForInformado() {
		assertThrows(
				ExcecaoDisponibilidadeInvalidaException.class,
				() -> excecaoService.cadastrar(
						1L,
						DATA,
						TipoExcecaoDisponibilidade.BLOQUEIO,
						LocalTime.of(12, 0),
						null
				)
		);

		verifyNoInteractions(profissionalService, excecaoRepository);
	}

	@Test
	void naoDeveCadastrarExcecaoDuplicada() {
		when(profissionalService.buscarPorId(1L))
				.thenReturn(novoProfissional());
		when(excecaoRepository
				.existsByProfissionalIdAndDataAndTipoAndHorarioInicioAndHorarioFimAndAtivoTrue(
						1L,
						DATA,
						TipoExcecaoDisponibilidade.BLOQUEIO,
						LocalTime.NOON,
						LocalTime.of(13, 0)
				))
				.thenReturn(true);

		assertThrows(
				ExcecaoDisponibilidadeJaCadastradaException.class,
				() -> excecaoService.cadastrar(
						1L,
						DATA,
						TipoExcecaoDisponibilidade.BLOQUEIO,
						LocalTime.NOON,
						LocalTime.of(13, 0)
				)
		);

		verify(excecaoRepository, never()).save(any());
	}

	@Test
	void deveListarExcecoesAtivasDaData() {
		ExcecaoDisponibilidade excecao = new ExcecaoDisponibilidade(
				novoProfissional(),
				DATA,
				TipoExcecaoDisponibilidade.BLOQUEIO,
				LocalTime.NOON,
				LocalTime.of(13, 0)
		);

		when(profissionalService.buscarPorId(1L))
				.thenReturn(novoProfissional());
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						1L,
						DATA
				))
				.thenReturn(List.of(excecao));

		List<ExcecaoDisponibilidade> resultado =
				excecaoService.listarAtivasPorProfissionalEData(1L, DATA);

		assertSame(excecao, resultado.get(0));
	}

	@Test
	void deveDesativarExcecaoExistente() {
		ExcecaoDisponibilidade excecao = new ExcecaoDisponibilidade(
				novoProfissional(),
				DATA,
				TipoExcecaoDisponibilidade.BLOQUEIO,
				null,
				null
		);

		when(excecaoRepository.findById(1L))
				.thenReturn(Optional.of(excecao));

		excecaoService.desativar(1L);

		assertFalse(excecao.isAtivo());
		verify(excecaoRepository, never()).save(any());
	}

	private Profissional novoProfissional() {
		return new Profissional(
				new Usuario("Ana", "ana@agendapro.com")
		);
	}
}
