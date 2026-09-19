package com.agendapro.agendamento.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.agendamento.entity.Agendamento;
import com.agendapro.agendamento.entity.StatusAgendamento;
import com.agendapro.agendamento.exception.AgendamentoNaoEncontradoException;
import com.agendapro.agendamento.exception.AtendimentoAindaNaoFinalizadoException;
import com.agendapro.agendamento.exception.FiltroAgendamentoInvalidoException;
import com.agendapro.agendamento.exception.HorarioIndisponivelException;
import com.agendapro.agendamento.exception.HorarioLocalInvalidoException;
import com.agendapro.agendamento.repository.AgendamentoRepository;
import com.agendapro.disponibilidade.dto.DisponibilidadeResponse;
import com.agendapro.disponibilidade.dto.HorarioDisponivelResponse;
import com.agendapro.disponibilidade.dto.SituacaoDisponibilidade;
import com.agendapro.disponibilidade.service.DisponibilidadeService;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.service.ProfissionalService;
import com.agendapro.servico.entity.Servico;
import com.agendapro.servico.service.ServicoService;
import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

	private static final Long CLIENTE_ID = 1L;
	private static final Long PROFISSIONAL_ID = 2L;
	private static final Long SERVICO_ID = 3L;
	private static final Long SERVICO_ADICIONAL_ID = 4L;
	private static final LocalDate DATA = LocalDate.of(2030, 1, 7);
	private static final LocalTime HORARIO = LocalTime.of(9, 0);

	@Mock
	private AgendamentoRepository agendamentoRepository;

	@Mock
	private UsuarioService usuarioService;

	@Mock
	private ProfissionalService profissionalService;

	@Mock
	private ServicoService servicoService;

	@Mock
	private DisponibilidadeService disponibilidadeService;

	@Mock
	private Clock clock;

	@InjectMocks
	private AgendamentoService agendamentoService;

	private Usuario cliente;
	private Profissional profissional;
	private Servico servico;
	private Barbearia barbearia;

	@BeforeEach
	void prepararEntidades() {
		lenient().when(clock.instant()).thenReturn(Instant.parse("2029-01-01T00:00:00Z"));
		cliente = new Usuario("Cliente", "cliente@agendapro.com");
		barbearia = new Barbearia("Barbearia");
		profissional = new Profissional(
				new Usuario("Ana", "ana@agendapro.com"),
				barbearia
		);
		servico = new Servico(
				"Barba",
				null,
				30,
				new BigDecimal("35.00"),
				barbearia
		);
	}

	@Test
	void deveCriarAgendamentoEmHorarioDisponivel() {
		prepararHorarioDisponivel();
		when(agendamentoRepository
				.existsByProfissionalIdAndStatusInAndInicioLessThanAndFimGreaterThan(
						PROFISSIONAL_ID,
						StatusAgendamento.QUE_OCUPAM_HORARIO,
						Instant.parse("2030-01-07T12:30:00Z"),
						Instant.parse("2030-01-07T12:00:00Z")
				))
				.thenReturn(false);
		when(agendamentoRepository.saveAndFlush(any(Agendamento.class)))
				.thenAnswer(invocacao -> invocacao.getArgument(0));

		Agendamento resultado = agendar();

		assertSame(cliente, resultado.getCliente());
		assertSame(profissional, resultado.getProfissional());
		assertSame(servico, resultado.getServico());
		assertEquals(Instant.parse("2030-01-07T12:00:00Z"), resultado.getInicio());
		assertEquals(Instant.parse("2030-01-07T12:30:00Z"), resultado.getFim());
		assertEquals(StatusAgendamento.AGENDADO, resultado.getStatus());
	}

	@Test
	void deveCriarAgendamentoComServicoAdicionalEDuracaoSomada() {
		Servico adicional = new Servico(
				"Barba", null, 30, new BigDecimal("30.00"), barbearia);
		when(usuarioService.buscarPorId(CLIENTE_ID)).thenReturn(cliente);
		when(disponibilidadeService.consultar(
				PROFISSIONAL_ID, SERVICO_ID, SERVICO_ADICIONAL_ID, DATA))
				.thenReturn(new DisponibilidadeResponse(
						PROFISSIONAL_ID,
						SERVICO_ID,
						DATA,
						SituacaoDisponibilidade.DISPONIVEL,
						List.of(new HorarioDisponivelResponse(
								HORARIO, LocalTime.of(10, 0)))
				));
		when(profissionalService.buscarPorId(PROFISSIONAL_ID))
				.thenReturn(profissional);
		when(servicoService.buscarPorId(SERVICO_ID)).thenReturn(servico);
		when(servicoService.buscarPorId(SERVICO_ADICIONAL_ID)).thenReturn(adicional);
		when(agendamentoRepository
				.existsByProfissionalIdAndStatusInAndInicioLessThanAndFimGreaterThan(
						PROFISSIONAL_ID,
						StatusAgendamento.QUE_OCUPAM_HORARIO,
						Instant.parse("2030-01-07T13:00:00Z"),
						Instant.parse("2030-01-07T12:00:00Z")))
				.thenReturn(false);
		when(agendamentoRepository.saveAndFlush(any(Agendamento.class)))
				.thenAnswer(invocacao -> invocacao.getArgument(0));

		Agendamento resultado = agendamentoService.agendar(
				CLIENTE_ID,
				PROFISSIONAL_ID,
				SERVICO_ID,
				SERVICO_ADICIONAL_ID,
				DATA,
				HORARIO
		);

		assertSame(adicional, resultado.getServicoAdicional());
		assertEquals(Instant.parse("2030-01-07T13:00:00Z"), resultado.getFim());
	}

	@Test
	void naoDeveAgendarHorarioForaDaDisponibilidade() {
		when(usuarioService.buscarPorId(CLIENTE_ID)).thenReturn(cliente);
		when(disponibilidadeService.consultar(
				PROFISSIONAL_ID,
				SERVICO_ID,
				DATA
		)).thenReturn(new DisponibilidadeResponse(
				PROFISSIONAL_ID,
				SERVICO_ID,
				DATA,
				SituacaoDisponibilidade.HORARIOS_OCUPADOS,
				List.of()
		));

		assertThrows(HorarioIndisponivelException.class, this::agendar);

		verifyNoInteractions(profissionalService, servicoService, agendamentoRepository);
	}

	@Test
	void naoDeveAgendarQuandoExistirSobreposicao() {
		prepararHorarioDisponivel();
		when(agendamentoRepository
				.existsByProfissionalIdAndStatusInAndInicioLessThanAndFimGreaterThan(
						any(),
						any(),
						any(),
						any()
				))
				.thenReturn(true);

		assertThrows(HorarioIndisponivelException.class, this::agendar);

		verify(agendamentoRepository, never()).saveAndFlush(any());
	}

	@Test
	void naoDeveAgendarHorarioNoPassado() {
		prepararHorarioDisponivel();
		when(clock.instant()).thenReturn(Instant.parse("2031-01-01T00:00:00Z"));

		assertThrows(HorarioIndisponivelException.class, this::agendar);

		verify(agendamentoRepository, never())
				.existsByProfissionalIdAndStatusInAndInicioLessThanAndFimGreaterThan(
						any(), any(), any(), any()
				);
	}

	@Test
	void deveTraduzirConflitoDoBancoParaHorarioIndisponivel() {
		prepararHorarioDisponivel();
		when(agendamentoRepository
				.existsByProfissionalIdAndStatusInAndInicioLessThanAndFimGreaterThan(
						any(),
						any(),
						any(),
						any()
				))
				.thenReturn(false);
		when(agendamentoRepository.saveAndFlush(any(Agendamento.class)))
				.thenThrow(new DataIntegrityViolationException("sobreposição"));

		assertThrows(HorarioIndisponivelException.class, this::agendar);
	}

	@Test
	void naoDeveAgendarHorarioInexistenteDuranteMudancaDeFuso() {
		LocalDate dataMudancaFuso = LocalDate.of(2024, 3, 10);
		LocalTime horarioInexistente = LocalTime.of(2, 30);
		profissional = new Profissional(
				new Usuario("Nova York", "ny@agendapro.com"),
				ZoneId.of("America/New_York")
		);

		when(usuarioService.buscarPorId(CLIENTE_ID)).thenReturn(cliente);
		when(disponibilidadeService.consultar(
				PROFISSIONAL_ID,
				SERVICO_ID,
				dataMudancaFuso
		)).thenReturn(new DisponibilidadeResponse(
				PROFISSIONAL_ID,
				SERVICO_ID,
				dataMudancaFuso,
				SituacaoDisponibilidade.DISPONIVEL,
				List.of(new HorarioDisponivelResponse(
						horarioInexistente,
						LocalTime.of(3, 0)
				))
		));
		when(profissionalService.buscarPorId(PROFISSIONAL_ID))
				.thenReturn(profissional);
		when(servicoService.buscarPorId(SERVICO_ID)).thenReturn(servico);

		assertThrows(
				HorarioLocalInvalidoException.class,
				() -> agendamentoService.agendar(
						CLIENTE_ID,
						PROFISSIONAL_ID,
						SERVICO_ID,
						dataMudancaFuso,
						horarioInexistente
				)
		);
	}

	@Test
	void deveBuscarAgendamentoPorId() {
		Agendamento agendamento = novoAgendamento();
		when(agendamentoRepository.findById(1L))
				.thenReturn(Optional.of(agendamento));

		assertSame(agendamento, agendamentoService.buscarPorId(1L));
	}

	@Test
	void deveLancarExcecaoAoBuscarAgendamentoInexistente() {
		when(agendamentoRepository.findById(999L))
				.thenReturn(Optional.empty());

		assertThrows(
				AgendamentoNaoEncontradoException.class,
				() -> agendamentoService.buscarPorId(999L)
		);
	}

	@Test
	void deveConfirmarAgendamentoSemSalvarManualmente() {
		Agendamento agendamento = novoAgendamento();
		when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

		Agendamento resultado = agendamentoService.confirmar(10L);

		assertSame(agendamento, resultado);
		assertEquals(StatusAgendamento.CONFIRMADO, resultado.getStatus());
		verify(agendamentoRepository, never()).save(any());
	}

	@Test
	void deveCancelarAgendamentoConfirmado() {
		Agendamento agendamento = novoAgendamento();
		agendamento.confirmar();
		when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

		Agendamento resultado = agendamentoService.cancelar(10L);

		assertEquals(StatusAgendamento.CANCELADO, resultado.getStatus());
	}

	@Test
	void deveConcluirAgendamentoConfirmado() {
		Agendamento agendamento = novoAgendamento();
		agendamento.confirmar();
		when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
		when(clock.instant()).thenReturn(Instant.parse("2031-01-01T00:00:00Z"));

		Agendamento resultado = agendamentoService.concluir(10L);

		assertEquals(StatusAgendamento.CONCLUIDO, resultado.getStatus());
	}

	@Test
	void naoDeveConcluirAntesDoFimDoAtendimento() {
		Agendamento agendamento = novoAgendamento();
		agendamento.confirmar();
		when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
		when(clock.instant()).thenReturn(Instant.parse("2030-01-07T12:15:00Z"));

		assertThrows(
				AtendimentoAindaNaoFinalizadoException.class,
				() -> agendamentoService.concluir(10L)
		);
		assertEquals(StatusAgendamento.CONFIRMADO, agendamento.getStatus());
	}

	@Test
	void deveFiltrarEOrdenarHistoricoDoProfissional() {
		Agendamento agendamento = novoAgendamento();
		PageRequest requisicao = PageRequest.of(
				1,
				10,
				Sort.by(Sort.Direction.ASC, "inicio")
		);
		when(profissionalService.buscarPorId(PROFISSIONAL_ID)).thenReturn(profissional);
		when(agendamentoRepository.findAll(
				org.mockito.ArgumentMatchers.<Specification<Agendamento>>any(),
				eq(requisicao)
		)).thenReturn(new PageImpl<>(List.of(agendamento), requisicao, 1));

		Page<Agendamento> resultado = agendamentoService.listarPorProfissional(
				PROFISSIONAL_ID,
				null,
				LocalDate.of(2030, 1, 7),
				LocalDate.of(2030, 1, 10),
				StatusAgendamento.CONFIRMADO,
				requisicao
		);

		assertSame(agendamento, resultado.getContent().get(0));
		assertEquals(1, resultado.getNumber());
	}

	@Test
	void naoDeveMisturarDataExataComIntervalo() {
		assertThrows(
				FiltroAgendamentoInvalidoException.class,
				() -> agendamentoService.listarPorProfissional(
						PROFISSIONAL_ID,
						DATA,
						DATA,
						null,
						null,
						PageRequest.of(0, 20)
				)
		);
		verifyNoInteractions(profissionalService);
	}

	@Test
	void naoDeveOrdenarPorCampoDesconhecido() {
		when(profissionalService.buscarPorId(PROFISSIONAL_ID)).thenReturn(profissional);

		assertThrows(
				FiltroAgendamentoInvalidoException.class,
				() -> agendamentoService.listarPorProfissional(
						PROFISSIONAL_ID,
						null,
						null,
						null,
						null,
						PageRequest.of(0, 20, Sort.by("cliente.senhaHash"))
				)
		);
		verify(agendamentoRepository, never()).findAll(
				org.mockito.ArgumentMatchers.<Specification<Agendamento>>any(),
				any(Pageable.class)
		);
	}

	@Test
	void deveListarHistoricoDoClienteComPeriodoEStatus() {
		Agendamento agendamento = novoAgendamento();
		PageRequest requisicao = PageRequest.of(0, 20);
		when(usuarioService.buscarPorId(CLIENTE_ID)).thenReturn(cliente);
		when(agendamentoRepository.findAll(
				org.mockito.ArgumentMatchers.<Specification<Agendamento>>any(),
				any(Pageable.class)
		)).thenReturn(new PageImpl<>(List.of(agendamento), requisicao, 1));

		Page<Agendamento> resultado = agendamentoService.listarPorCliente(
				CLIENTE_ID,
				OffsetDateTime.parse("2030-01-01T00:00:00-03:00"),
				OffsetDateTime.parse("2030-02-01T00:00:00-03:00"),
				StatusAgendamento.CONFIRMADO,
				requisicao
		);

		assertEquals(1, resultado.getTotalElements());
		assertSame(agendamento, resultado.getContent().get(0));
	}

	@Test
	void deveListarAgendaAdministrativaComFiltrosEPaginacao() {
		Agendamento agendamento = novoAgendamento();
		PageRequest requisicao = PageRequest.of(
				0,
				25,
				Sort.by(Sort.Direction.DESC, "inicio")
		);
		when(profissionalService.buscarPorId(PROFISSIONAL_ID)).thenReturn(profissional);
		when(agendamentoRepository.findAll(
				org.mockito.ArgumentMatchers.<Specification<Agendamento>>any(),
				eq(requisicao)
		)).thenReturn(new PageImpl<>(List.of(agendamento), requisicao, 1));

		Page<Agendamento> resultado = agendamentoService.listarParaAdministracao(
				PROFISSIONAL_ID,
				OffsetDateTime.parse("2030-01-01T00:00:00-03:00"),
				OffsetDateTime.parse("2030-02-01T00:00:00-03:00"),
				StatusAgendamento.CONFIRMADO,
				requisicao
		);

		assertEquals(1, resultado.getTotalElements());
		assertSame(agendamento, resultado.getContent().get(0));
		verify(profissionalService).buscarPorId(PROFISSIONAL_ID);
	}

	@Test
	void naoDeveListarAgendaAdministrativaComIntervaloInvertido() {
		assertThrows(
				FiltroAgendamentoInvalidoException.class,
				() -> agendamentoService.listarParaAdministracao(
						null,
						OffsetDateTime.parse("2030-02-01T00:00:00-03:00"),
						OffsetDateTime.parse("2030-01-01T00:00:00-03:00"),
						null,
						PageRequest.of(0, 20)
				)
		);

		verifyNoInteractions(profissionalService);
		verify(agendamentoRepository, never()).findAll(
				org.mockito.ArgumentMatchers.<Specification<Agendamento>>any(),
				any(Pageable.class)
		);
	}

	@Test
	void deveListarAgendamentosPeloDiaLocalDoProfissional() {
		Agendamento agendamento = novoAgendamento();
		when(profissionalService.buscarPorId(PROFISSIONAL_ID))
				.thenReturn(profissional);
		when(agendamentoRepository
				.findAllByProfissionalIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicio(
						PROFISSIONAL_ID,
						Instant.parse("2030-01-07T03:00:00Z"),
						Instant.parse("2030-01-08T03:00:00Z")
				))
				.thenReturn(List.of(agendamento));

		List<Agendamento> resultado = agendamentoService
				.listarPorProfissionalEData(PROFISSIONAL_ID, DATA);

		assertSame(agendamento, resultado.get(0));
	}

	private void prepararHorarioDisponivel() {
		when(usuarioService.buscarPorId(CLIENTE_ID)).thenReturn(cliente);
		when(disponibilidadeService.consultar(
				PROFISSIONAL_ID,
				SERVICO_ID,
				DATA
		)).thenReturn(new DisponibilidadeResponse(
				PROFISSIONAL_ID,
				SERVICO_ID,
				DATA,
				SituacaoDisponibilidade.DISPONIVEL,
				List.of(new HorarioDisponivelResponse(
						HORARIO,
						LocalTime.of(9, 30)
				))
		));
		when(profissionalService.buscarPorId(PROFISSIONAL_ID))
				.thenReturn(profissional);
		when(servicoService.buscarPorId(SERVICO_ID)).thenReturn(servico);
	}

	private Agendamento agendar() {
		return agendamentoService.agendar(
				CLIENTE_ID,
				PROFISSIONAL_ID,
				SERVICO_ID,
				DATA,
				HORARIO
		);
	}

	private Agendamento novoAgendamento() {
		return new Agendamento(
				cliente,
				profissional,
				servico,
				Instant.parse("2030-01-07T12:00:00Z"),
				Instant.parse("2030-01-07T12:30:00Z")
		);
	}
}
