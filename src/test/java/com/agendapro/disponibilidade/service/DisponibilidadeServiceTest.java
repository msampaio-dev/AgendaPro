package com.agendapro.disponibilidade.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agendapro.agendamento.entity.Agendamento;
import com.agendapro.agendamento.entity.StatusAgendamento;
import com.agendapro.agendamento.repository.AgendamentoRepository;
import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.barbearia.entity.HorarioFuncionamentoBarbearia;
import com.agendapro.barbearia.repository.HorarioFuncionamentoBarbeariaRepository;
import com.agendapro.disponibilidade.dto.DisponibilidadeResponse;
import com.agendapro.disponibilidade.dto.SituacaoDisponibilidade;
import com.agendapro.disponibilidade.entity.ExcecaoDisponibilidade;
import com.agendapro.disponibilidade.entity.HorarioAtendimento;
import com.agendapro.disponibilidade.entity.TipoExcecaoDisponibilidade;
import com.agendapro.disponibilidade.repository.ExcecaoDisponibilidadeRepository;
import com.agendapro.disponibilidade.repository.HorarioAtendimentoRepository;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.service.ProfissionalService;
import com.agendapro.profissionalservico.service.ProfissionalServicoService;
import com.agendapro.servico.entity.Servico;
import com.agendapro.servico.service.ServicoService;
import com.agendapro.usuario.entity.Usuario;

@ExtendWith(MockitoExtension.class)
class DisponibilidadeServiceTest {

	private static final Long PROFISSIONAL_ID = 1L;
	private static final Long SERVICO_ID = 2L;
	private static final Long SERVICO_ADICIONAL_ID = 3L;
	private static final LocalDate DATA = LocalDate.of(2030, 1, 7);

	@Mock
	private ProfissionalService profissionalService;

	@Mock
	private ServicoService servicoService;

	@Mock
	private ProfissionalServicoService profissionalServicoService;

	@Mock
	private HorarioAtendimentoRepository horarioRepository;

	@Mock
	private HorarioFuncionamentoBarbeariaRepository funcionamentoRepository;

	@Mock
	private ExcecaoDisponibilidadeRepository excecaoRepository;

	@Mock
	private AgendamentoRepository agendamentoRepository;

	@Mock
	private Clock clock;

	@InjectMocks
	private DisponibilidadeService disponibilidadeService;

	private Profissional profissional;

	@BeforeEach
	void prepararProfissional() {
		lenient().when(clock.instant())
				.thenReturn(Instant.parse("2029-01-01T00:00:00Z"));
		profissional = new Profissional(
				new Usuario("Ana", "ana@agendapro.com")
		);
	}

	@Test
	void naoDeveExibirHorariosQueJaPassaramNoDiaAtual() {
		prepararServicoComDuracao(30);
		when(clock.instant()).thenReturn(Instant.parse("2030-01-07T13:15:00Z"));
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA.getDayOfWeek()
				))
				.thenReturn(List.of(horario(9, 0, 12, 0)));
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA
				))
				.thenReturn(List.of());

		DisponibilidadeResponse resultado = consultar();

		assertEquals(3, resultado.horarios().size());
		assertEquals(SituacaoDisponibilidade.DISPONIVEL, resultado.situacao());
		assertEquals(LocalTime.of(10, 30), resultado.horarios().get(0).inicio());
		assertEquals(LocalTime.of(11, 30), resultado.horarios().get(2).inicio());
	}

	@Test
	void intervaloDeAlmocoNaoDeveGerarHorarioQueAtravesseAPausa() {
		prepararServicoComDuracao(60);
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA.getDayOfWeek()
				))
				.thenReturn(List.of(
						horario(9, 0, 12, 0),
						horario(13, 0, 18, 0)
				));
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA
				))
				.thenReturn(List.of());

		DisponibilidadeResponse resultado = consultar();

		assertEquals(8, resultado.horarios().size());
		assertEquals(LocalTime.of(11, 0), resultado.horarios().get(2).inicio());
		assertEquals(LocalTime.of(13, 0), resultado.horarios().get(3).inicio());
	}

	@Test
	void deveRestringirAgendaAoHorarioDeFuncionamentoDaBarbearia() {
		Barbearia barbearia = org.mockito.Mockito.mock(Barbearia.class);
		when(barbearia.getId()).thenReturn(10L);
		when(barbearia.isAtivo()).thenReturn(true);
		profissional = new Profissional(
				new Usuario("Ana", "ana@agendapro.com"), barbearia);
		prepararServicoComDuracao(60);
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID, DATA.getDayOfWeek()))
				.thenReturn(List.of(horario(8, 0, 18, 0)));
		when(funcionamentoRepository
				.findAllByBarbeariaIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						10L, DATA.getDayOfWeek()))
				.thenReturn(List.of(
						new HorarioFuncionamentoBarbearia(
								barbearia, DATA.getDayOfWeek(),
								LocalTime.of(9, 0), LocalTime.of(12, 0)),
						new HorarioFuncionamentoBarbearia(
								barbearia, DATA.getDayOfWeek(),
								LocalTime.of(13, 0), LocalTime.of(17, 0))));
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID, DATA))
				.thenReturn(List.of());

		DisponibilidadeResponse resultado = consultar();

		assertEquals(7, resultado.horarios().size());
		assertEquals(LocalTime.of(9, 0), resultado.horarios().get(0).inicio());
		assertEquals(LocalTime.of(13, 0), resultado.horarios().get(3).inicio());
		assertEquals(LocalTime.of(16, 0), resultado.horarios().get(6).inicio());
	}

	@Test
	void devePermitirHorariosAdjacentesAoAgendamentoExistente() {
		prepararServicoComDuracao(30);
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA.getDayOfWeek()
				))
				.thenReturn(List.of(horario(9, 0, 10, 30)));
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA
				))
				.thenReturn(List.of());
		Agendamento ocupado = new Agendamento(
				new Usuario("Cliente", "cliente2@agendapro.com"),
				profissional,
				novoServico(30),
				Instant.parse("2030-01-07T12:30:00Z"),
				Instant.parse("2030-01-07T13:00:00Z")
		);
		when(agendamentoRepository
				.findAllByProfissionalIdAndStatusInAndInicioLessThanAndFimGreaterThanOrderByInicio(
						PROFISSIONAL_ID,
						StatusAgendamento.QUE_OCUPAM_HORARIO,
						Instant.parse("2030-01-08T03:00:00Z"),
						Instant.parse("2030-01-07T03:00:00Z")
				))
				.thenReturn(List.of(ocupado));

		DisponibilidadeResponse resultado = consultar();

		assertEquals(2, resultado.horarios().size());
		assertEquals(LocalTime.of(9, 0), resultado.horarios().get(0).inicio());
		assertEquals(LocalTime.of(10, 0), resultado.horarios().get(1).inicio());
	}

	@Test
	void deveEncontrarSomenteProximasDatasComHorariosDisponiveis() {
		prepararServicoComDuracao(30);
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						org.mockito.ArgumentMatchers.eq(PROFISSIONAL_ID),
						org.mockito.ArgumentMatchers.any()
				))
				.thenAnswer(invocacao -> invocacao.getArgument(1) == java.time.DayOfWeek.MONDAY
						? List.of(horario(9, 0, 10, 0))
						: List.of());
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						org.mockito.ArgumentMatchers.eq(PROFISSIONAL_ID),
						org.mockito.ArgumentMatchers.any()
				))
				.thenReturn(List.of());

		List<DisponibilidadeResponse> resultado =
				disponibilidadeService.consultarProximasDatas(
						PROFISSIONAL_ID,
						SERVICO_ID,
						DATA,
						2,
						8
				);

		assertEquals(2, resultado.size());
		assertEquals(LocalDate.of(2030, 1, 7), resultado.get(0).data());
		assertEquals(LocalDate.of(2030, 1, 14), resultado.get(1).data());
	}

	@Test
	void deveGerarHorariosComBaseNaDuracaoDoServico() {
		prepararServicoComDuracao(30);
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA.getDayOfWeek()
				))
				.thenReturn(List.of(horario(9, 0, 12, 0)));
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA
				))
				.thenReturn(List.of());

		DisponibilidadeResponse resultado = consultar();

		assertEquals(6, resultado.horarios().size());
		assertEquals(LocalTime.of(9, 0), resultado.horarios().get(0).inicio());
		assertEquals(LocalTime.of(9, 30), resultado.horarios().get(0).fim());
		assertEquals(LocalTime.of(11, 30), resultado.horarios().get(5).inicio());
		assertEquals(LocalTime.of(12, 0), resultado.horarios().get(5).fim());
		verify(profissionalServicoService)
				.validarAssociacaoAtiva(PROFISSIONAL_ID, SERVICO_ID);
	}

	@Test
	void deveSomarDuracaoDoServicoAdicionalAoCalcularHorarios() {
		prepararServicoComDuracao(30);
		when(servicoService.buscarPorId(SERVICO_ADICIONAL_ID))
				.thenReturn(novoServico(30));
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID, DATA.getDayOfWeek()))
				.thenReturn(List.of(horario(9, 0, 12, 0)));
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID, DATA))
				.thenReturn(List.of());

		DisponibilidadeResponse resultado = disponibilidadeService.consultar(
				PROFISSIONAL_ID, SERVICO_ID, SERVICO_ADICIONAL_ID, DATA);

		assertEquals(3, resultado.horarios().size());
		assertEquals(LocalTime.of(9, 0), resultado.horarios().get(0).inicio());
		assertEquals(LocalTime.of(10, 0), resultado.horarios().get(0).fim());
		verify(profissionalServicoService)
				.validarAssociacaoAtiva(PROFISSIONAL_ID, SERVICO_ID);
		verify(profissionalServicoService)
				.validarAssociacaoAtiva(PROFISSIONAL_ID, SERVICO_ADICIONAL_ID);
	}

	@Test
	void deveRemoverBloqueioParcialDosHorarios() {
		prepararServicoComDuracao(30);
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA.getDayOfWeek()
				))
				.thenReturn(List.of(horario(9, 0, 12, 0)));
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA
				))
				.thenReturn(List.of(excecao(
						TipoExcecaoDisponibilidade.BLOQUEIO,
						10,
						0,
						11,
						0
				)));

		DisponibilidadeResponse resultado = consultar();

		assertEquals(4, resultado.horarios().size());
		assertEquals(LocalTime.of(9, 0), resultado.horarios().get(0).inicio());
		assertEquals(LocalTime.of(11, 0), resultado.horarios().get(2).inicio());
	}

	@Test
	void bloqueioDeDiaInteiroDeveRemoverTodaDisponibilidade() {
		prepararServicoComDuracao(30);
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA.getDayOfWeek()
				))
				.thenReturn(List.of(horario(9, 0, 12, 0)));
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA
				))
				.thenReturn(List.of(new ExcecaoDisponibilidade(
						profissional,
						DATA,
						TipoExcecaoDisponibilidade.BLOQUEIO,
						null,
						null
				)));

		DisponibilidadeResponse resultado = consultar();

		assertTrue(resultado.horarios().isEmpty());
		assertEquals(SituacaoDisponibilidade.DIA_BLOQUEADO, resultado.situacao());
	}

	@Test
	void deveGerarHorariosEmDisponibilidadeExtraSemHorarioSemanal() {
		prepararServicoComDuracao(30);
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA.getDayOfWeek()
				))
				.thenReturn(List.of());
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA
				))
				.thenReturn(List.of(excecao(
						TipoExcecaoDisponibilidade.DISPONIBILIDADE_EXTRA,
						18,
						0,
						19,
						0
				)));

		DisponibilidadeResponse resultado = consultar();

		assertEquals(2, resultado.horarios().size());
		assertEquals(LocalTime.of(18, 0), resultado.horarios().get(0).inicio());
		assertEquals(LocalTime.of(19, 0), resultado.horarios().get(1).fim());
	}

	@Test
	void deveDescartarSobraMenorQueDuracaoDoServico() {
		prepararServicoComDuracao(45);
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA.getDayOfWeek()
				))
				.thenReturn(List.of(horario(9, 0, 10, 0)));
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA
				))
				.thenReturn(List.of());

		DisponibilidadeResponse resultado = consultar();

		assertEquals(1, resultado.horarios().size());
		assertEquals(LocalTime.of(9, 0), resultado.horarios().get(0).inicio());
		assertEquals(LocalTime.of(9, 45), resultado.horarios().get(0).fim());
	}

	@Test
	void deveRemoverHorarioOcupadoPorAgendamentoAtivo() {
		prepararServicoComDuracao(30);
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA.getDayOfWeek()
				))
				.thenReturn(List.of(horario(9, 0, 11, 0)));
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID,
						DATA
				))
				.thenReturn(List.of());
		Agendamento ocupado = new Agendamento(
				new Usuario("Cliente", "cliente@agendapro.com"),
				profissional,
				novoServico(30),
				Instant.parse("2030-01-07T12:30:00Z"),
				Instant.parse("2030-01-07T13:00:00Z")
		);
		when(agendamentoRepository
				.findAllByProfissionalIdAndStatusInAndInicioLessThanAndFimGreaterThanOrderByInicio(
						PROFISSIONAL_ID,
						StatusAgendamento.QUE_OCUPAM_HORARIO,
						Instant.parse("2030-01-08T03:00:00Z"),
						Instant.parse("2030-01-07T03:00:00Z")
				))
				.thenReturn(List.of(ocupado));

		DisponibilidadeResponse resultado = consultar();

		assertEquals(3, resultado.horarios().size());
		assertEquals(LocalTime.of(9, 0), resultado.horarios().get(0).inicio());
		assertEquals(LocalTime.of(10, 0), resultado.horarios().get(1).inicio());
		assertEquals(LocalTime.of(10, 30), resultado.horarios().get(2).inicio());
	}

	@Test
	void deveExplicarQuandoProfissionalNaoTemExpediente() {
		prepararServicoComDuracao(30);
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID, DATA.getDayOfWeek()))
				.thenReturn(List.of());
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID, DATA))
				.thenReturn(List.of());

		assertEquals(SituacaoDisponibilidade.SEM_EXPEDIENTE, consultar().situacao());
	}

	@Test
	void deveExplicarQuandoServicoNaoCabeNoIntervaloLivre() {
		prepararServicoComDuracao(90);
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID, DATA.getDayOfWeek()))
				.thenReturn(List.of(horario(9, 0, 10, 0)));
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID, DATA))
				.thenReturn(List.of());

		assertEquals(SituacaoDisponibilidade.SEM_ENCAIXE, consultar().situacao());
	}

	@Test
	void deveExplicarQuandoTodosOsHorariosDoDiaJaEncerraram() {
		prepararServicoComDuracao(30);
		when(clock.instant()).thenReturn(Instant.parse("2030-01-07T15:00:00Z"));
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID, DATA.getDayOfWeek()))
				.thenReturn(List.of(horario(9, 0, 10, 0)));
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID, DATA))
				.thenReturn(List.of());

		assertEquals(
				SituacaoDisponibilidade.HORARIOS_ENCERRADOS,
				consultar().situacao()
		);
	}

	@Test
	void deveExplicarQuandoTodosOsHorariosFuturosEstaoOcupados() {
		prepararServicoComDuracao(30);
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID, DATA.getDayOfWeek()))
				.thenReturn(List.of(horario(9, 0, 10, 0)));
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID, DATA))
				.thenReturn(List.of());
		Agendamento ocupado = new Agendamento(
				new Usuario("Cliente", "ocupado@agendapro.com"),
				profissional,
				novoServico(60),
				Instant.parse("2030-01-07T12:00:00Z"),
				Instant.parse("2030-01-07T13:00:00Z")
		);
		when(agendamentoRepository
				.findAllByProfissionalIdAndStatusInAndInicioLessThanAndFimGreaterThanOrderByInicio(
						PROFISSIONAL_ID,
						StatusAgendamento.QUE_OCUPAM_HORARIO,
						Instant.parse("2030-01-08T03:00:00Z"),
						Instant.parse("2030-01-07T03:00:00Z")))
				.thenReturn(List.of(ocupado));

		assertEquals(
				SituacaoDisponibilidade.HORARIOS_OCUPADOS,
				consultar().situacao()
		);
	}

	@Test
	void deveDescartarHorarioInexistenteDuranteMudancaDoRelogio() {
		LocalDate dataMudancaRelogio = LocalDate.of(2030, 3, 10);
		profissional = new Profissional(
				new Usuario("Nova York", "ny@agendapro.com"),
				ZoneId.of("America/New_York")
		);
		prepararServicoComDuracao(30);
		when(horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID, dataMudancaRelogio.getDayOfWeek()))
				.thenReturn(List.of(new HorarioAtendimento(
						profissional,
						dataMudancaRelogio.getDayOfWeek(),
						LocalTime.of(2, 0),
						LocalTime.of(3, 0)
				)));
		when(excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						PROFISSIONAL_ID, dataMudancaRelogio))
				.thenReturn(List.of());

		DisponibilidadeResponse resultado = disponibilidadeService.consultar(
				PROFISSIONAL_ID, SERVICO_ID, dataMudancaRelogio);

		assertEquals(
				SituacaoDisponibilidade.HORARIO_LOCAL_INVALIDO,
				resultado.situacao()
		);
	}

	private void prepararServicoComDuracao(int duracaoMinutos) {
		Servico servico = novoServico(duracaoMinutos);

		when(profissionalService.buscarPorId(PROFISSIONAL_ID))
				.thenReturn(profissional);
		when(servicoService.buscarPorId(SERVICO_ID)).thenReturn(servico);
	}

	private Servico novoServico(int duracaoMinutos) {
		return new Servico(
				"Barba",
				null,
				duracaoMinutos,
				new BigDecimal("35.00"),
				new Barbearia("Barbearia")
		);
	}

	private DisponibilidadeResponse consultar() {
		return disponibilidadeService.consultar(
				PROFISSIONAL_ID,
				SERVICO_ID,
				DATA
		);
	}

	private HorarioAtendimento horario(
			int horaInicio,
			int minutoInicio,
			int horaFim,
			int minutoFim
	) {
		return new HorarioAtendimento(
				profissional,
				DATA.getDayOfWeek(),
				LocalTime.of(horaInicio, minutoInicio),
				LocalTime.of(horaFim, minutoFim)
		);
	}

	private ExcecaoDisponibilidade excecao(
			TipoExcecaoDisponibilidade tipo,
			int horaInicio,
			int minutoInicio,
			int horaFim,
			int minutoFim
	) {
		return new ExcecaoDisponibilidade(
				profissional,
				DATA,
				tipo,
				LocalTime.of(horaInicio, minutoInicio),
				LocalTime.of(horaFim, minutoFim)
		);
	}
}
