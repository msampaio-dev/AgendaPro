package com.agendapro.sugestao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agendapro.disponibilidade.dto.DisponibilidadeResponse;
import com.agendapro.disponibilidade.dto.HorarioDisponivelResponse;
import com.agendapro.disponibilidade.dto.SituacaoDisponibilidade;
import com.agendapro.disponibilidade.service.DisponibilidadeService;
import com.agendapro.sugestao.dto.SugestaoAgendamentoResponse;
import com.agendapro.sugestao.exception.LimiteSugestoesAtingidoException;
import com.agendapro.sugestao.exception.PedidoNaoInterpretadoException;
import com.agendapro.sugestao.exception.SugestaoIndisponivelException;
import com.agendapro.sugestao.ia.CatalogoSugestao;
import com.agendapro.sugestao.ia.InterpretadorPedido;
import com.agendapro.sugestao.ia.PedidoInterpretado;
import com.agendapro.sugestao.ia.PedidoInterpretado.Periodo;

/**
 * O interpretador e simulado: cada teste decide o que "o Claude respondeu" e
 * confere o que o servico faz com isso, inclusive quando a resposta e invalida.
 */
@ExtendWith(MockitoExtension.class)
class SugestaoAgendamentoServiceTest {

	private static final Long USUARIO_ID = 1L;
	private static final String TEXTO = "corte e barba sexta a tarde com o joao";

	/** Quarta-feira, 7 de outubro de 2026, 10h em Sao Paulo. */
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T13:00:00Z"), ZoneOffset.UTC);
	private static final LocalDate HOJE = LocalDate.of(2026, 10, 7);
	private static final LocalDate SEXTA = LocalDate.of(2026, 10, 9);

	private static final Long IPANEMA = 1L;
	private static final Long MORUMBI = 2L;
	private static final Long JOAO = 10L;
	private static final Long VICTOR = 11L;
	private static final Long HENRIQUE = 20L;
	private static final Long CORTE = 100L;
	private static final Long BARBA = 101L;
	private static final Long CORTE_MORUMBI = 200L;

	private static final CatalogoSugestao CATALOGO = new CatalogoSugestao(HOJE, List.of(
			new CatalogoSugestao.Barbearia(IPANEMA, "Barbershopping Ipanema",
					List.of(
							new CatalogoSugestao.Profissional(JOAO, "João Gabriel", List.of(CORTE, BARBA)),
							new CatalogoSugestao.Profissional(VICTOR, "Victor Ruiz", List.of(CORTE))),
					List.of(
							new CatalogoSugestao.Servico(CORTE, "Corte de cabelo Social", 30),
							new CatalogoSugestao.Servico(BARBA, "Barba", 30))),
			new CatalogoSugestao.Barbearia(MORUMBI, "Barbershop Morumbi",
					List.of(new CatalogoSugestao.Profissional(HENRIQUE, "Henrique Silva", List.of(CORTE_MORUMBI))),
					List.of(new CatalogoSugestao.Servico(CORTE_MORUMBI, "Corte de cabelo Social", 30)))
	));

	@Mock
	private LimiteUsoSugestao limiteUso;

	@Mock
	private CatalogoSugestaoService catalogoService;

	@Mock
	private InterpretadorPedido interpretador;

	@Mock
	private DisponibilidadeService disponibilidadeService;

	private SugestaoAgendamentoService service;

	@BeforeEach
	void configurar() {
		service = new SugestaoAgendamentoService(
				limiteUso, catalogoService, interpretador, disponibilidadeService, CLOCK);
	}

	@Test
	void deveSugerirHorariosDaTardeDoDiaPedido() {
		prepararChamada(pedido(IPANEMA, JOAO, CORTE, BARBA, "2026-10-09", Periodo.TARDE));
		when(disponibilidadeService.consultar(JOAO, CORTE, BARBA, SEXTA))
				.thenReturn(disponibilidade(SEXTA, "09:00", "13:00", "16:00", "18:00"));

		SugestaoAgendamentoResponse sugestao = service.sugerir(USUARIO_ID, TEXTO);

		assertEquals(IPANEMA, sugestao.barbeariaId());
		assertEquals("João Gabriel", sugestao.profissionalNome());
		assertEquals("Corte de cabelo Social", sugestao.servicoNome());
		assertEquals("Barba", sugestao.servicoAdicionalNome());
		assertEquals(SEXTA, sugestao.data());
		assertEquals(List.of(LocalTime.of(13, 0), LocalTime.of(16, 0)), inicios(sugestao));
		assertEquals(7, sugestao.sugestoesRestantesHoje());
	}

	@Test
	void deveProcurarAProximaDataComVagaQuandoODiaPedidoNaoTemHorarioNoPeriodo() {
		prepararChamada(pedido(IPANEMA, JOAO, CORTE, null, "2026-10-09", Periodo.TARDE));
		when(disponibilidadeService.consultar(JOAO, CORTE, null, SEXTA))
				.thenReturn(disponibilidade(SEXTA, "09:00"));
		LocalDate sabado = SEXTA.plusDays(1);
		when(disponibilidadeService.consultarProximasDatas(eq(JOAO), eq(CORTE), eq(null), eq(sabado), anyInt(), anyInt()))
				.thenReturn(List.of(disponibilidade(sabado, "14:00")));

		SugestaoAgendamentoResponse sugestao = service.sugerir(USUARIO_ID, TEXTO);

		assertEquals(SEXTA, sugestao.dataPedida());
		assertEquals(sabado, sugestao.data());
		assertEquals(List.of(LocalTime.of(14, 0)), inicios(sugestao));
	}

	@Test
	void deveTestarOsProfissionaisQueFazemOServicoQuandoOClienteNaoCitaNenhum() {
		prepararChamada(pedido(null, null, CORTE, null, null, Periodo.QUALQUER));
		when(disponibilidadeService.consultarProximasDatas(eq(JOAO), eq(CORTE), eq(null), eq(HOJE), anyInt(), anyInt()))
				.thenReturn(List.of());
		when(disponibilidadeService.consultarProximasDatas(eq(VICTOR), eq(CORTE), eq(null), eq(HOJE), anyInt(), anyInt()))
				.thenReturn(List.of(disponibilidade(HOJE, "15:00")));

		SugestaoAgendamentoResponse sugestao = service.sugerir(USUARIO_ID, "quero cortar o cabelo");

		assertEquals(VICTOR, sugestao.profissionalId());
		assertNull(sugestao.dataPedida());
	}

	@Test
	void deveDevolverSugestaoSemHorariosQuandoNaoHaVagaNenhuma() {
		prepararChamada(pedido(IPANEMA, JOAO, CORTE, null, "2026-10-09", Periodo.QUALQUER));
		when(disponibilidadeService.consultar(JOAO, CORTE, null, SEXTA))
				.thenReturn(disponibilidade(SEXTA));
		when(disponibilidadeService.consultarProximasDatas(anyLong(), anyLong(), any(), any(), anyInt(), anyInt()))
				.thenReturn(List.of());

		SugestaoAgendamentoResponse sugestao = service.sugerir(USUARIO_ID, TEXTO);

		assertEquals(JOAO, sugestao.profissionalId());
		assertTrue(sugestao.horarios().isEmpty());
	}

	@Test
	void deveRecusarServicoInventadoPelaIa() {
		prepararChamada(pedido(IPANEMA, JOAO, 999L, null, null, Periodo.QUALQUER));

		assertThrows(PedidoNaoInterpretadoException.class, () -> service.sugerir(USUARIO_ID, TEXTO));
		verifyNoInteractions(disponibilidadeService);
		verify(limiteUso, never()).devolverUso(any(), any());
	}

	@Test
	void deveRecusarProfissionalInventadoPelaIa() {
		prepararChamada(pedido(IPANEMA, 999L, CORTE, null, null, Periodo.QUALQUER));

		assertThrows(PedidoNaoInterpretadoException.class, () -> service.sugerir(USUARIO_ID, TEXTO));
		verifyNoInteractions(disponibilidadeService);
	}

	@Test
	void deveRecusarProfissionalDeOutraBarbearia() {
		prepararChamada(pedido(IPANEMA, HENRIQUE, CORTE, null, null, Periodo.QUALQUER));

		assertThrows(PedidoNaoInterpretadoException.class, () -> service.sugerir(USUARIO_ID, TEXTO));
	}

	@Test
	void deveRecusarBarbeariaQueNaoEADoServico() {
		prepararChamada(pedido(MORUMBI, null, CORTE, null, null, Periodo.QUALQUER));

		assertThrows(PedidoNaoInterpretadoException.class, () -> service.sugerir(USUARIO_ID, TEXTO));
	}

	@Test
	void deveRecusarProfissionalQueNaoFazOServicoAdicional() {
		prepararChamada(pedido(IPANEMA, VICTOR, CORTE, BARBA, null, Periodo.QUALQUER));

		PedidoNaoInterpretadoException erro = assertThrows(PedidoNaoInterpretadoException.class,
				() -> service.sugerir(USUARIO_ID, TEXTO));
		assertTrue(erro.getMessage().contains("não faz esse serviço"));
	}

	@Test
	void deveRecusarServicoAdicionalIgualAoPrincipal() {
		prepararChamada(pedido(IPANEMA, JOAO, CORTE, CORTE, null, Periodo.QUALQUER));

		assertThrows(PedidoNaoInterpretadoException.class, () -> service.sugerir(USUARIO_ID, TEXTO));
	}

	@Test
	void deveRecusarPedidoSemServico() {
		prepararChamada(pedido(IPANEMA, JOAO, null, null, "2026-10-09", Periodo.TARDE));

		assertThrows(PedidoNaoInterpretadoException.class, () -> service.sugerir(USUARIO_ID, "oi"));
	}

	@Test
	void deveRecusarDataEmFormatoInvalido() {
		prepararChamada(pedido(IPANEMA, JOAO, CORTE, null, "sexta-feira", Periodo.TARDE));

		assertThrows(PedidoNaoInterpretadoException.class, () -> service.sugerir(USUARIO_ID, TEXTO));
	}

	@Test
	void deveRecusarDataNoPassado() {
		prepararChamada(pedido(IPANEMA, JOAO, CORTE, null, "2026-10-06", Periodo.QUALQUER));

		assertThrows(PedidoNaoInterpretadoException.class, () -> service.sugerir(USUARIO_ID, TEXTO));
		verifyNoInteractions(disponibilidadeService);
	}

	@Test
	void deveRecusarDataAlemDoHorizonte() {
		prepararChamada(pedido(IPANEMA, JOAO, CORTE, null, "2027-03-01", Periodo.QUALQUER));

		assertThrows(PedidoNaoInterpretadoException.class, () -> service.sugerir(USUARIO_ID, TEXTO));
	}

	@Test
	void deveRecusarRespostaComCampoAusente() {
		prepararChamada(new PedidoInterpretado(
				Optional.of(IPANEMA), null, Optional.of(CORTE), Optional.empty(),
				Optional.empty(), Periodo.QUALQUER, ""));

		assertThrows(PedidoNaoInterpretadoException.class, () -> service.sugerir(USUARIO_ID, TEXTO));
	}

	@Test
	void deveDevolverUsoQuandoAIaFicaIndisponivelSemCobranca() {
		when(limiteUso.registrarUso(USUARIO_ID)).thenReturn(new LimiteUsoSugestao.UsoRegistrado(HOJE, 9));
		when(catalogoService.montar(HOJE)).thenReturn(CATALOGO);
		when(interpretador.interpretar(TEXTO, CATALOGO)).thenThrow(SugestaoIndisponivelException.semCobranca());

		assertThrows(SugestaoIndisponivelException.class, () -> service.sugerir(USUARIO_ID, TEXTO));
		verify(limiteUso).devolverUso(USUARIO_ID, HOJE);
	}

	@Test
	void deveDevolverUsoQuandoOCatalogoNaoPodeSerMontado() {
		when(limiteUso.registrarUso(USUARIO_ID)).thenReturn(new LimiteUsoSugestao.UsoRegistrado(HOJE, 9));
		when(catalogoService.montar(HOJE)).thenThrow(new IllegalStateException("banco indisponivel"));

		assertThrows(IllegalStateException.class, () -> service.sugerir(USUARIO_ID, TEXTO));
		verify(limiteUso).devolverUso(USUARIO_ID, HOJE);
		verify(interpretador, never()).interpretar(any(), any());
	}

	@Test
	void naoDeveDevolverUsoQuandoAChamadaPodeTerSidoCobrada() {
		when(limiteUso.registrarUso(USUARIO_ID)).thenReturn(new LimiteUsoSugestao.UsoRegistrado(HOJE, 9));
		when(catalogoService.montar(HOJE)).thenReturn(CATALOGO);
		when(interpretador.interpretar(TEXTO, CATALOGO)).thenThrow(SugestaoIndisponivelException.possivelmenteCobrada());

		assertThrows(SugestaoIndisponivelException.class, () -> service.sugerir(USUARIO_ID, TEXTO));
		verify(limiteUso, never()).devolverUso(any(), any());
	}

	@Test
	void naoDeveDevolverUsoQuandoAIaRespondeAlgoInvalido() {
		when(limiteUso.registrarUso(USUARIO_ID)).thenReturn(new LimiteUsoSugestao.UsoRegistrado(HOJE, 9));
		when(catalogoService.montar(HOJE)).thenReturn(CATALOGO);
		when(interpretador.interpretar(TEXTO, CATALOGO)).thenThrow(new PedidoNaoInterpretadoException());

		assertThrows(PedidoNaoInterpretadoException.class, () -> service.sugerir(USUARIO_ID, TEXTO));
		verify(limiteUso, never()).devolverUso(any(), any());
	}

	@Test
	void naoDeveChamarAIaQuandoOLimiteEstoura() {
		when(limiteUso.registrarUso(USUARIO_ID)).thenThrow(new LimiteSugestoesAtingidoException());

		assertThrows(LimiteSugestoesAtingidoException.class, () -> service.sugerir(USUARIO_ID, TEXTO));
		verify(interpretador, never()).interpretar(any(), any());
	}

	@Test
	void deveCortarObservacaoLongaDoModelo() {
		String redacao = "Era uma vez ".repeat(100);

		String cortada = SugestaoAgendamentoService.limitarObservacao(redacao);

		assertTrue(cortada.length() <= SugestaoAgendamentoService.MAXIMO_CARACTERES_OBSERVACAO + 1);
		assertTrue(cortada.endsWith("…"));
		assertEquals("Escolhi o corte social.", SugestaoAgendamentoService.limitarObservacao(" Escolhi o corte social. "));
		assertEquals("", SugestaoAgendamentoService.limitarObservacao(null));
	}

	@Test
	void deveFiltrarHorariosPorPeriodoNasFronteiras() {
		List<HorarioDisponivelResponse> horarios = horarios("11:30", "12:00", "17:30", "18:00");

		assertEquals(horarios("11:30"), SugestaoAgendamentoService.filtrarPorPeriodo(horarios, Periodo.MANHA));
		assertEquals(horarios("12:00", "17:30"), SugestaoAgendamentoService.filtrarPorPeriodo(horarios, Periodo.TARDE));
		assertEquals(horarios("18:00"), SugestaoAgendamentoService.filtrarPorPeriodo(horarios, Periodo.NOITE));
		assertEquals(horarios, SugestaoAgendamentoService.filtrarPorPeriodo(horarios, Periodo.QUALQUER));
	}

	private void prepararChamada(PedidoInterpretado pedido) {
		when(limiteUso.registrarUso(USUARIO_ID)).thenReturn(new LimiteUsoSugestao.UsoRegistrado(HOJE, 7));
		when(catalogoService.montar(HOJE)).thenReturn(CATALOGO);
		when(interpretador.interpretar(any(), eq(CATALOGO))).thenReturn(pedido);
	}

	private static PedidoInterpretado pedido(
			Long barbeariaId,
			Long profissionalId,
			Long servicoId,
			Long servicoAdicionalId,
			String data,
			Periodo periodo
	) {
		return new PedidoInterpretado(
				Optional.ofNullable(barbeariaId),
				Optional.ofNullable(profissionalId),
				Optional.ofNullable(servicoId),
				Optional.ofNullable(servicoAdicionalId),
				Optional.ofNullable(data),
				periodo,
				""
		);
	}

	private static DisponibilidadeResponse disponibilidade(LocalDate data, String... inicios) {
		List<HorarioDisponivelResponse> horarios = horarios(inicios);
		return new DisponibilidadeResponse(
				JOAO, CORTE, data,
				horarios.isEmpty() ? SituacaoDisponibilidade.HORARIOS_OCUPADOS : SituacaoDisponibilidade.DISPONIVEL,
				horarios);
	}

	private static List<HorarioDisponivelResponse> horarios(String... inicios) {
		return java.util.Arrays.stream(inicios)
				.map(LocalTime::parse)
				.map(inicio -> new HorarioDisponivelResponse(inicio, inicio.plusMinutes(30)))
				.toList();
	}

	private static List<LocalTime> inicios(SugestaoAgendamentoResponse sugestao) {
		return sugestao.horarios().stream().map(HorarioDisponivelResponse::inicio).toList();
	}
}
