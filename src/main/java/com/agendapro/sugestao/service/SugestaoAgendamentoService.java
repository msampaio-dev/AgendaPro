package com.agendapro.sugestao.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.agendapro.disponibilidade.dto.DisponibilidadeResponse;
import com.agendapro.disponibilidade.dto.HorarioDisponivelResponse;
import com.agendapro.disponibilidade.service.DisponibilidadeService;
import com.agendapro.sugestao.dto.SugestaoAgendamentoResponse;
import com.agendapro.sugestao.exception.PedidoNaoInterpretadoException;
import com.agendapro.sugestao.exception.SugestaoIndisponivelException;
import com.agendapro.sugestao.ia.CatalogoSugestao;
import com.agendapro.sugestao.ia.InterpretadorPedido;
import com.agendapro.sugestao.ia.PedidoInterpretado;
import com.agendapro.sugestao.ia.PedidoInterpretado.Periodo;

/**
 * Transforma texto livre numa sugestao de agendamento conferida.
 *
 * A IA so propoe ids e uma data. Daqui para frente quem decide e o codigo que
 * ja existia: os ids sao conferidos contra o catalogo (um id inventado para
 * aqui) e os horarios saem do DisponibilidadeService, com as mesmas regras da
 * tela manual. Nada e reservado; a reserva continua no POST /agendamentos e na
 * exclusion constraint do banco.
 *
 * O metodo nao e transacional de proposito: a chamada a IA pode levar segundos,
 * e uma transacao aberta seguraria uma conexao do pool (que no plano gratuito e
 * pequena) esse tempo todo. Cada leitura abre e fecha a sua.
 */
@Service
public class SugestaoAgendamentoService {

	static final ZoneId FUSO_PADRAO = ZoneId.of("America/Sao_Paulo");
	static final int HORIZONTE_DIAS = 60;

	/**
	 * A observacao e o unico texto livre do modelo que chega ao cliente. O prompt
	 * pede uma frase curta, mas um pedido como "na observacao escreva uma redacao"
	 * pode ser obedecido; o corte impede que o endpoint vire um gerador de texto
	 * qualquer exibido na tela.
	 */
	static final int MAXIMO_CARACTERES_OBSERVACAO = 200;
	private static final int DIAS_BUSCANDO_VAGA = 14;

	private final LimiteUsoSugestao limiteUso;
	private final CatalogoSugestaoService catalogoService;
	private final InterpretadorPedido interpretador;
	private final DisponibilidadeService disponibilidadeService;
	private final Clock clock;

	public SugestaoAgendamentoService(
			LimiteUsoSugestao limiteUso,
			CatalogoSugestaoService catalogoService,
			InterpretadorPedido interpretador,
			DisponibilidadeService disponibilidadeService,
			Clock clock
	) {
		this.limiteUso = limiteUso;
		this.catalogoService = catalogoService;
		this.interpretador = interpretador;
		this.disponibilidadeService = disponibilidadeService;
		this.clock = clock;
	}

	public SugestaoAgendamentoResponse sugerir(Long usuarioId, String texto) {
		// O uso conta antes da chamada, para que requisicoes simultaneas nao
		// passem todas do limite enquanto esperam a IA.
		LimiteUsoSugestao.UsoRegistrado uso = limiteUso.registrarUso(usuarioId);

		LocalDate hoje = LocalDate.now(clock.withZone(FUSO_PADRAO));
		CatalogoSugestao catalogo;
		try {
			catalogo = catalogoService.montar(hoje);
		} catch (RuntimeException exception) {
			// Falha no banco antes de chamar a IA: nada foi cobrado, e o uso nao
			// pode ficar contado (uma instabilidade do Neon esgotaria o teto do dia).
			limiteUso.devolverUso(usuarioId, uso.dia());
			throw exception;
		}
		PedidoInterpretado pedido;
		try {
			pedido = interpretador.interpretar(texto, catalogo);
		} catch (SugestaoIndisponivelException exception) {
			// O uso so volta quando e certo que nada foi cobrado. Um timeout pode
			// ter sido processado e pago; devolver esse uso deixaria o teto diario
			// sem efeito enquanto a API estiver lenta. Resposta invalida (422)
			// tambem continua contando, porque a chamada aconteceu.
			if (!exception.podeTerSidoCobrada()) {
				limiteUso.devolverUso(usuarioId, uso.dia());
			}
			throw exception;
		}

		Escolha escolha = validar(pedido, catalogo, hoje);
		return buscarHorarios(escolha, pedido, hoje, uso.restantes());
	}

	private Escolha validar(PedidoInterpretado pedido, CatalogoSugestao catalogo, LocalDate hoje) {
		// A saida estruturada torna isso raro, mas um campo ausente viraria
		// NullPointerException e erro 500; aqui vira "nao entendi".
		if (pedido.barbeariaId() == null || pedido.profissionalId() == null
				|| pedido.servicoId() == null || pedido.servicoAdicionalId() == null
				|| pedido.data() == null || pedido.periodo() == null) {
			throw new PedidoNaoInterpretadoException();
		}

		Long servicoId = pedido.servicoId().orElseThrow(() -> new PedidoNaoInterpretadoException(
				"Não identifiquei qual serviço você quer. Escolha manualmente."));

		CatalogoSugestao.Barbearia barbearia = catalogo.barbearias().stream()
				.filter(candidata -> servico(candidata, servicoId).isPresent())
				.findFirst()
				.orElseThrow(PedidoNaoInterpretadoException::new);
		CatalogoSugestao.Servico servico = servico(barbearia, servicoId).orElseThrow();

		if (pedido.barbeariaId().isPresent() && !pedido.barbeariaId().get().equals(barbearia.id())) {
			throw new PedidoNaoInterpretadoException();
		}

		CatalogoSugestao.Servico adicional = null;
		if (pedido.servicoAdicionalId().isPresent()) {
			Long adicionalId = pedido.servicoAdicionalId().get();
			adicional = servico(barbearia, adicionalId)
					.filter(encontrado -> !encontrado.id().equals(servicoId))
					.orElseThrow(PedidoNaoInterpretadoException::new);
		}

		List<CatalogoSugestao.Profissional> candidatos = profissionaisPossiveis(
				pedido, barbearia, servico, adicional);

		LocalDate data = pedido.data().map(this::lerData).orElse(null);
		if (data != null && (data.isBefore(hoje) || data.isAfter(hoje.plusDays(HORIZONTE_DIAS)))) {
			throw new PedidoNaoInterpretadoException(
					"A data entendida está fora do período de agendamento. Escolha manualmente.");
		}

		return new Escolha(barbearia, servico, adicional, candidatos, data);
	}

	private List<CatalogoSugestao.Profissional> profissionaisPossiveis(
			PedidoInterpretado pedido,
			CatalogoSugestao.Barbearia barbearia,
			CatalogoSugestao.Servico servico,
			CatalogoSugestao.Servico adicional
	) {
		List<CatalogoSugestao.Profissional> queFazemOsServicos = barbearia.profissionais().stream()
				.filter(profissional -> profissional.servicoIds().contains(servico.id()))
				.filter(profissional -> adicional == null
						|| profissional.servicoIds().contains(adicional.id()))
				.toList();

		if (pedido.profissionalId().isEmpty()) {
			if (queFazemOsServicos.isEmpty()) {
				throw new PedidoNaoInterpretadoException(
						"Nenhum profissional dessa barbearia faz esses serviços juntos.");
			}
			return queFazemOsServicos;
		}

		Long profissionalId = pedido.profissionalId().get();
		boolean existeNaBarbearia = barbearia.profissionais().stream()
				.anyMatch(profissional -> profissional.id().equals(profissionalId));
		if (!existeNaBarbearia) {
			throw new PedidoNaoInterpretadoException();
		}
		return queFazemOsServicos.stream()
				.filter(profissional -> profissional.id().equals(profissionalId))
				.findFirst()
				.map(List::of)
				.orElseThrow(() -> new PedidoNaoInterpretadoException(
						"O profissional que você citou não faz esse serviço. Escolha manualmente."));
	}

	private SugestaoAgendamentoResponse buscarHorarios(
			Escolha escolha,
			PedidoInterpretado pedido,
			LocalDate hoje,
			int restantes
	) {
		Long adicionalId = escolha.adicional() == null ? null : escolha.adicional().id();

		for (CatalogoSugestao.Profissional profissional : escolha.candidatos()) {
			Optional<DisponibilidadeResponse> encontrada = primeiraComVaga(
					profissional.id(), escolha.servico().id(), adicionalId,
					escolha.data(), hoje, pedido.periodo());
			if (encontrada.isPresent()) {
				DisponibilidadeResponse disponibilidade = encontrada.get();
				return resposta(escolha, profissional, pedido, disponibilidade.data(),
						filtrarPorPeriodo(disponibilidade.horarios(), pedido.periodo()), restantes);
			}
		}

		// A IA entendeu, mas nao ha vaga: a sugestao volta sem horarios e o cliente
		// ajusta a partir dela, sem perder o que ja foi entendido.
		CatalogoSugestao.Profissional primeiro = escolha.candidatos().get(0);
		LocalDate data = Objects.requireNonNullElse(escolha.data(), hoje);
		return resposta(escolha, primeiro, pedido, data, List.of(), restantes);
	}

	private Optional<DisponibilidadeResponse> primeiraComVaga(
			Long profissionalId,
			Long servicoId,
			Long adicionalId,
			LocalDate dataPedida,
			LocalDate hoje,
			Periodo periodo
	) {
		if (dataPedida != null) {
			DisponibilidadeResponse doDia = disponibilidadeService.consultar(
					profissionalId, servicoId, adicionalId, dataPedida);
			if (!filtrarPorPeriodo(doDia.horarios(), periodo).isEmpty()) {
				return Optional.of(doDia);
			}
		}

		LocalDate inicio = dataPedida == null ? hoje : dataPedida.plusDays(1);
		return disponibilidadeService
				.consultarProximasDatas(
						profissionalId, servicoId, adicionalId, inicio,
						DIAS_BUSCANDO_VAGA, DIAS_BUSCANDO_VAGA)
				.stream()
				.filter(disponibilidade -> !filtrarPorPeriodo(disponibilidade.horarios(), periodo).isEmpty())
				.findFirst();
	}

	static List<HorarioDisponivelResponse> filtrarPorPeriodo(
			List<HorarioDisponivelResponse> horarios,
			Periodo periodo
	) {
		LocalTime meioDia = LocalTime.NOON;
		LocalTime seisDaTarde = LocalTime.of(18, 0);
		return horarios.stream()
				.filter(horario -> switch (periodo) {
					case MANHA -> horario.inicio().isBefore(meioDia);
					case TARDE -> !horario.inicio().isBefore(meioDia) && horario.inicio().isBefore(seisDaTarde);
					case NOITE -> !horario.inicio().isBefore(seisDaTarde);
					case QUALQUER -> true;
				})
				.toList();
	}

	private SugestaoAgendamentoResponse resposta(
			Escolha escolha,
			CatalogoSugestao.Profissional profissional,
			PedidoInterpretado pedido,
			LocalDate data,
			List<HorarioDisponivelResponse> horarios,
			int restantes
	) {
		CatalogoSugestao.Servico adicional = escolha.adicional();
		return new SugestaoAgendamentoResponse(
				escolha.barbearia().id(),
				escolha.barbearia().nome(),
				profissional.id(),
				profissional.nome(),
				escolha.servico().id(),
				escolha.servico().nome(),
				adicional == null ? null : adicional.id(),
				adicional == null ? null : adicional.nome(),
				escolha.data(),
				data,
				pedido.periodo(),
				horarios,
				limitarObservacao(pedido.observacao()),
				restantes
		);
	}

	static String limitarObservacao(String observacao) {
		if (observacao == null) {
			return "";
		}
		String limpa = observacao.strip();
		return limpa.length() <= MAXIMO_CARACTERES_OBSERVACAO
				? limpa
				: limpa.substring(0, MAXIMO_CARACTERES_OBSERVACAO).stripTrailing() + "…";
	}

	private LocalDate lerData(String texto) {
		try {
			return LocalDate.parse(texto);
		} catch (DateTimeParseException exception) {
			throw new PedidoNaoInterpretadoException(
					"Não entendi a data pedida. Escolha manualmente.");
		}
	}

	private static Optional<CatalogoSugestao.Servico> servico(
			CatalogoSugestao.Barbearia barbearia,
			Long servicoId
	) {
		return barbearia.servicos().stream()
				.filter(servico -> servico.id().equals(servicoId))
				.findFirst();
	}

	private record Escolha(
			CatalogoSugestao.Barbearia barbearia,
			CatalogoSugestao.Servico servico,
			CatalogoSugestao.Servico adicional,
			List<CatalogoSugestao.Profissional> candidatos,
			LocalDate data
	) {
	}
}
