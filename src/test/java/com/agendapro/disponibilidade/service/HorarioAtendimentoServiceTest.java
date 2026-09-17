package com.agendapro.disponibilidade.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agendapro.disponibilidade.entity.HorarioAtendimento;
import com.agendapro.disponibilidade.exception.HorarioAtendimentoInvalidoException;
import com.agendapro.disponibilidade.exception.HorarioAtendimentoSobrepostoException;
import com.agendapro.disponibilidade.repository.HorarioAtendimentoRepository;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.exception.ProfissionalInativoException;
import com.agendapro.profissional.service.ProfissionalService;
import com.agendapro.usuario.entity.Usuario;

@ExtendWith(MockitoExtension.class)
class HorarioAtendimentoServiceTest {

	@Mock
	private HorarioAtendimentoRepository horarioAtendimentoRepository;

	@Mock
	private ProfissionalService profissionalService;

	@InjectMocks
	private HorarioAtendimentoService horarioAtendimentoService;

	@Test
	void deveCadastrarHorarioValidoSemSobreposicao() {
		Long profissionalId = 1L;
		Profissional profissional = novoProfissional();

		when(profissionalService.buscarPorId(profissionalId))
				.thenReturn(profissional);
		when(horarioAtendimentoRepository
				.existsByProfissionalIdAndDiaSemanaAndAtivoTrueAndHorarioInicioLessThanAndHorarioFimGreaterThan(
						profissionalId,
						DayOfWeek.MONDAY,
						LocalTime.of(12, 0),
						LocalTime.of(9, 0)
				))
				.thenReturn(false);
		when(horarioAtendimentoRepository.save(any(HorarioAtendimento.class)))
				.thenAnswer(invocacao -> invocacao.getArgument(0));

		HorarioAtendimento resultado = horarioAtendimentoService.cadastrar(
				profissionalId,
				DayOfWeek.MONDAY,
				LocalTime.of(9, 0),
				LocalTime.of(12, 0)
		);

		assertSame(profissional, resultado.getProfissional());
		assertEquals(DayOfWeek.MONDAY, resultado.getDiaSemana());
		assertEquals(LocalTime.of(9, 0), resultado.getHorarioInicio());
		assertEquals(LocalTime.of(12, 0), resultado.getHorarioFim());
		assertTrue(resultado.isAtivo());
	}

	@Test
	void naoDeveCadastrarIntervaloComInicioIgualAoFim() {
		assertThrows(
				HorarioAtendimentoInvalidoException.class,
				() -> horarioAtendimentoService.cadastrar(
						1L,
						DayOfWeek.MONDAY,
						LocalTime.of(9, 0),
						LocalTime.of(9, 0)
				)
		);

		verifyNoInteractions(profissionalService, horarioAtendimentoRepository);
	}

	@Test
	void deveCadastrarJornadaComIntervaloEmDoisPeriodos() {
		Long profissionalId = 1L;
		Profissional profissional = novoProfissional();

		when(profissionalService.buscarPorId(profissionalId))
				.thenReturn(profissional);

		List<HorarioAtendimento> resultado =
				horarioAtendimentoService.cadastrarJornadaComIntervalo(
						profissionalId,
						DayOfWeek.MONDAY,
						LocalTime.of(9, 0),
						LocalTime.of(12, 0),
						LocalTime.of(13, 0),
						LocalTime.of(18, 0)
				);

		assertEquals(2, resultado.size());
		assertEquals(LocalTime.of(9, 0), resultado.get(0).getHorarioInicio());
		assertEquals(LocalTime.of(12, 0), resultado.get(0).getHorarioFim());
		assertEquals(LocalTime.of(13, 0), resultado.get(1).getHorarioInicio());
		assertEquals(LocalTime.of(18, 0), resultado.get(1).getHorarioFim());
		verify(horarioAtendimentoRepository).saveAll(resultado);
	}

	@Test
	void naoDeveCadastrarJornadaComIntervaloForaDoExpediente() {
		assertThrows(
				HorarioAtendimentoInvalidoException.class,
				() -> horarioAtendimentoService.cadastrarJornadaComIntervalo(
						1L,
						DayOfWeek.MONDAY,
						LocalTime.of(9, 0),
						LocalTime.of(8, 0),
						LocalTime.of(9, 0),
						LocalTime.of(18, 0)
				)
		);

		verifyNoInteractions(profissionalService, horarioAtendimentoRepository);
	}

	@Test
	void naoDeveCadastrarParaProfissionalInativo() {
		Profissional profissional = novoProfissional();
		profissional.desativar();

		when(profissionalService.buscarPorId(1L)).thenReturn(profissional);

		assertThrows(
				ProfissionalInativoException.class,
				() -> horarioAtendimentoService.cadastrar(
						1L,
						DayOfWeek.MONDAY,
						LocalTime.of(9, 0),
						LocalTime.of(12, 0)
				)
		);

		verifyNoInteractions(horarioAtendimentoRepository);
	}

	@Test
	void naoDeveCadastrarHorarioSobreposto() {
		when(profissionalService.buscarPorId(1L))
				.thenReturn(novoProfissional());
		when(horarioAtendimentoRepository
				.existsByProfissionalIdAndDiaSemanaAndAtivoTrueAndHorarioInicioLessThanAndHorarioFimGreaterThan(
						1L,
						DayOfWeek.MONDAY,
						LocalTime.of(11, 0),
						LocalTime.of(10, 0)
				))
				.thenReturn(true);

		assertThrows(
				HorarioAtendimentoSobrepostoException.class,
				() -> horarioAtendimentoService.cadastrar(
						1L,
						DayOfWeek.MONDAY,
						LocalTime.of(10, 0),
						LocalTime.of(11, 0)
				)
		);

		verify(horarioAtendimentoRepository, never())
				.save(any(HorarioAtendimento.class));
	}

	@Test
	void deveListarHorariosOrdenadosPorDiaEHora() {
		Profissional profissional = novoProfissional();
		HorarioAtendimento terca = novoHorario(
				profissional,
				DayOfWeek.TUESDAY,
				9,
				12
		);
		HorarioAtendimento segundaTarde = novoHorario(
				profissional,
				DayOfWeek.MONDAY,
				14,
				18
		);
		HorarioAtendimento segundaManha = novoHorario(
				profissional,
				DayOfWeek.MONDAY,
				8,
				12
		);

		when(profissionalService.buscarPorId(1L)).thenReturn(profissional);
		when(horarioAtendimentoRepository
				.findAllByProfissionalIdAndAtivoTrue(1L))
				.thenReturn(List.of(terca, segundaTarde, segundaManha));

		List<HorarioAtendimento> resultado =
				horarioAtendimentoService.listarAtivosPorProfissional(1L);

		assertSame(segundaManha, resultado.get(0));
		assertSame(segundaTarde, resultado.get(1));
		assertSame(terca, resultado.get(2));
	}

	@Test
	void deveDesativarHorarioExistente() {
		HorarioAtendimento horario = novoHorario(
				novoProfissional(),
				DayOfWeek.MONDAY,
				9,
				12
		);

		when(horarioAtendimentoRepository.findById(1L))
				.thenReturn(Optional.of(horario));

		horarioAtendimentoService.desativar(1L);

		assertFalse(horario.isAtivo());
		verify(horarioAtendimentoRepository, never()).save(any());
	}

	private Profissional novoProfissional() {
		return new Profissional(
				new Usuario("Ana", "ana@agendapro.com")
		);
	}

	private HorarioAtendimento novoHorario(
			Profissional profissional,
			DayOfWeek dia,
			int inicio,
			int fim
	) {
		return new HorarioAtendimento(
				profissional,
				dia,
				LocalTime.of(inicio, 0),
				LocalTime.of(fim, 0)
		);
	}
}
