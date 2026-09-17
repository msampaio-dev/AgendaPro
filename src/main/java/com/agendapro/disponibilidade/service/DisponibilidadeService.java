package com.agendapro.disponibilidade.service;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.agendamento.entity.Agendamento;
import com.agendapro.agendamento.entity.StatusAgendamento;
import com.agendapro.agendamento.repository.AgendamentoRepository;
import com.agendapro.agendamento.exception.ServicoAdicionalInvalidoException;
import com.agendapro.barbearia.exception.BarbeariaInativaException;
import com.agendapro.barbearia.repository.HorarioFuncionamentoBarbeariaRepository;
import com.agendapro.disponibilidade.dto.DisponibilidadeResponse;
import com.agendapro.disponibilidade.dto.HorarioDisponivelResponse;
import com.agendapro.disponibilidade.dto.SituacaoDisponibilidade;
import com.agendapro.disponibilidade.entity.ExcecaoDisponibilidade;
import com.agendapro.disponibilidade.entity.TipoExcecaoDisponibilidade;
import com.agendapro.disponibilidade.repository.ExcecaoDisponibilidadeRepository;
import com.agendapro.disponibilidade.repository.HorarioAtendimentoRepository;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.exception.ProfissionalInativoException;
import com.agendapro.profissional.service.ProfissionalService;
import com.agendapro.profissionalservico.service.ProfissionalServicoService;
import com.agendapro.servico.entity.Servico;
import com.agendapro.servico.exception.ServicoInativoException;
import com.agendapro.servico.service.ServicoService;

@Service
public class DisponibilidadeService {

	private final ProfissionalService profissionalService;
	private final ServicoService servicoService;
	private final ProfissionalServicoService profissionalServicoService;
	private final HorarioAtendimentoRepository horarioRepository;
	private final HorarioFuncionamentoBarbeariaRepository funcionamentoRepository;
	private final ExcecaoDisponibilidadeRepository excecaoRepository;
	private final AgendamentoRepository agendamentoRepository;
	private final Clock clock;

	public DisponibilidadeService(
			ProfissionalService profissionalService,
			ServicoService servicoService,
			ProfissionalServicoService profissionalServicoService,
			HorarioAtendimentoRepository horarioRepository,
			HorarioFuncionamentoBarbeariaRepository funcionamentoRepository,
			ExcecaoDisponibilidadeRepository excecaoRepository,
			AgendamentoRepository agendamentoRepository,
			Clock clock
	) {
		this.profissionalService = profissionalService;
		this.servicoService = servicoService;
		this.profissionalServicoService = profissionalServicoService;
		this.horarioRepository = horarioRepository;
		this.funcionamentoRepository = funcionamentoRepository;
		this.excecaoRepository = excecaoRepository;
		this.agendamentoRepository = agendamentoRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public DisponibilidadeResponse consultar(
			Long profissionalId,
			Long servicoId,
			LocalDate data
	) {
		return consultar(profissionalId, servicoId, null, data);
	}

	@Transactional(readOnly = true)
	public DisponibilidadeResponse consultar(
			Long profissionalId,
			Long servicoId,
			Long servicoAdicionalId,
			LocalDate data
	) {
		Contexto contexto = validarContexto(profissionalId, servicoId, servicoAdicionalId);
		return calcular(
				contexto.profissionalId(),
				contexto.servicoId(),
				contexto.profissional(),
				contexto.duracaoMinutos(),
				data
		);
	}

	@Transactional(readOnly = true)
	public List<DisponibilidadeResponse> consultarProximasDatas(
			Long profissionalId,
			Long servicoId,
			LocalDate dataInicial,
			int quantidade,
			int horizonteDias
	) {
		return consultarProximasDatas(
				profissionalId, servicoId, null, dataInicial, quantidade, horizonteDias);
	}

	@Transactional(readOnly = true)
	public List<DisponibilidadeResponse> consultarProximasDatas(
			Long profissionalId,
			Long servicoId,
			Long servicoAdicionalId,
			LocalDate dataInicial,
			int quantidade,
			int horizonteDias
	) {
		Contexto contexto = validarContexto(profissionalId, servicoId, servicoAdicionalId);
		List<DisponibilidadeResponse> proximas = new ArrayList<>();

		for (int deslocamento = 0;
				deslocamento < horizonteDias && proximas.size() < quantidade;
				deslocamento++) {
			LocalDate data = dataInicial.plusDays(deslocamento);
			DisponibilidadeResponse disponibilidade = calcular(
					contexto.profissionalId(),
					contexto.servicoId(),
					contexto.profissional(),
					contexto.duracaoMinutos(),
					data
			);

			if (!disponibilidade.horarios().isEmpty()) {
				proximas.add(disponibilidade);
			}
		}

		return List.copyOf(proximas);
	}

	private DisponibilidadeResponse calcular(
			Long profissionalId,
			Long servicoId,
			Profissional profissional,
			int duracaoMinutos,
			LocalDate data
	) {

		List<Intervalo> intervalos = new ArrayList<>();

		horarioRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						profissionalId,
						data.getDayOfWeek()
				)
				.stream()
				.map(horario -> new Intervalo(
						horario.getHorarioInicio(),
						horario.getHorarioFim()
				))
				.forEach(intervalos::add);

		List<ExcecaoDisponibilidade> excecoes = excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						profissionalId,
						data
				);

		boolean bloqueioDiaInteiro = excecoes.stream()
				.anyMatch(excecao ->
						excecao.getTipo() == TipoExcecaoDisponibilidade.BLOQUEIO
						&& excecao.isDiaInteiro()
				);

		if (bloqueioDiaInteiro) {
			return resposta(profissionalId, servicoId, data,
					SituacaoDisponibilidade.DIA_BLOQUEADO, List.of());
		}

		excecoes.stream()
				.filter(excecao ->
						excecao.getTipo()
								== TipoExcecaoDisponibilidade.DISPONIBILIDADE_EXTRA
				)
				.map(excecao -> new Intervalo(
						excecao.getHorarioInicio(),
						excecao.getHorarioFim()
				))
				.forEach(intervalos::add);

		List<Intervalo> disponiveis = mesclar(intervalos);
		if (profissional.getBarbearia() != null && profissional.getBarbearia().getId() != null) {
			List<Intervalo> funcionamento = funcionamentoRepository
					.findAllByBarbeariaIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
							profissional.getBarbearia().getId(), data.getDayOfWeek())
					.stream()
					.map(horario -> new Intervalo(horario.getHorarioInicio(), horario.getHorarioFim()))
					.toList();
			disponiveis = intersectar(disponiveis, funcionamento);
		}

		if (disponiveis.isEmpty()) {
			return resposta(profissionalId, servicoId, data,
					SituacaoDisponibilidade.SEM_EXPEDIENTE, List.of());
		}

		for (ExcecaoDisponibilidade excecao : excecoes) {
			if (excecao.getTipo() == TipoExcecaoDisponibilidade.BLOQUEIO) {
				disponiveis = subtrair(
						disponiveis,
						new Intervalo(
								excecao.getHorarioInicio(),
								excecao.getHorarioFim()
						)
				);
			}
		}

		if (disponiveis.isEmpty()) {
			return resposta(profissionalId, servicoId, data,
					SituacaoDisponibilidade.DIA_BLOQUEADO, List.of());
		}

		List<HorarioDisponivelResponse> horariosCalculados = gerarHorarios(
				disponiveis,
				duracaoMinutos
		);
		if (horariosCalculados.isEmpty()) {
			return resposta(profissionalId, servicoId, data,
					SituacaoDisponibilidade.SEM_ENCAIXE, List.of());
		}

		ResultadoHorarios resultado = removerHorariosOcupados(
				horariosCalculados,
				profissionalId,
				profissional,
				data,
				duracaoMinutos
		);

		return resposta(profissionalId, servicoId, data,
				resultado.situacao(), resultado.horarios());
	}

	private List<Intervalo> mesclar(List<Intervalo> intervalos) {
		if (intervalos.isEmpty()) {
			return List.of();
		}

		List<Intervalo> ordenados = intervalos.stream()
				.sorted(Comparator.comparing(Intervalo::inicio))
				.toList();
		List<Intervalo> resultado = new ArrayList<>();
		Intervalo atual = ordenados.get(0);

		for (int indice = 1; indice < ordenados.size(); indice++) {
			Intervalo proximo = ordenados.get(indice);

			if (!proximo.inicio().isAfter(atual.fim())) {
				LocalTime maiorFim = proximo.fim().isAfter(atual.fim())
						? proximo.fim()
						: atual.fim();
				atual = new Intervalo(atual.inicio(), maiorFim);
			} else {
				resultado.add(atual);
				atual = proximo;
			}
		}

		resultado.add(atual);
		return resultado;
	}

	private List<Intervalo> subtrair(
			List<Intervalo> disponiveis,
			Intervalo bloqueio
	) {
		List<Intervalo> resultado = new ArrayList<>();

		for (Intervalo disponivel : disponiveis) {
			boolean sobrepoe = disponivel.inicio().isBefore(bloqueio.fim())
					&& disponivel.fim().isAfter(bloqueio.inicio());

			if (!sobrepoe) {
				resultado.add(disponivel);
				continue;
			}

			if (disponivel.inicio().isBefore(bloqueio.inicio())) {
				resultado.add(
						new Intervalo(disponivel.inicio(), bloqueio.inicio())
				);
			}

			if (disponivel.fim().isAfter(bloqueio.fim())) {
				resultado.add(new Intervalo(bloqueio.fim(), disponivel.fim()));
			}
		}

		return resultado;
	}

	private List<Intervalo> intersectar(List<Intervalo> profissionais, List<Intervalo> funcionamento) {
		List<Intervalo> resultado = new ArrayList<>();
		for (Intervalo profissional : profissionais) {
			for (Intervalo abertura : funcionamento) {
				LocalTime inicio = profissional.inicio().isAfter(abertura.inicio())
						? profissional.inicio() : abertura.inicio();
				LocalTime fim = profissional.fim().isBefore(abertura.fim())
						? profissional.fim() : abertura.fim();
				if (inicio.isBefore(fim)) resultado.add(new Intervalo(inicio, fim));
			}
		}
		return mesclar(resultado);
	}

	private List<HorarioDisponivelResponse> gerarHorarios(
			List<Intervalo> intervalos,
			int duracaoMinutos
	) {
		List<HorarioDisponivelResponse> horarios = new ArrayList<>();

		for (Intervalo intervalo : intervalos) {
			LocalTime inicio = intervalo.inicio();
			LocalTime fim = inicio.plusMinutes(duracaoMinutos);

			while (!fim.isAfter(intervalo.fim())) {
				horarios.add(new HorarioDisponivelResponse(inicio, fim));
				inicio = fim;
				fim = inicio.plusMinutes(duracaoMinutos);
			}
		}

		return List.copyOf(horarios);
	}

	private ResultadoHorarios removerHorariosOcupados(
			List<HorarioDisponivelResponse> horarios,
			Long profissionalId,
			Profissional profissional,
			LocalDate data,
			int duracaoMinutos
	) {
		ZoneId fusoHorario = profissional.getFusoHorario();
		Instant inicioDoDia = data.atStartOfDay(fusoHorario).toInstant();
		Instant inicioDoProximoDia = data
				.plusDays(1)
				.atStartOfDay(fusoHorario)
				.toInstant();
		List<Agendamento> ocupados = agendamentoRepository
				.findAllByProfissionalIdAndStatusInAndInicioLessThanAndFimGreaterThanOrderByInicio(
						profissionalId,
						StatusAgendamento.QUE_OCUPAM_HORARIO,
						inicioDoProximoDia,
						inicioDoDia
				);
		Instant agora = clock.instant();

		List<HorarioComIntervalo> convertidos = horarios.stream()
				.map(horario -> converterIntervalo(
						data,
						horario,
						fusoHorario,
						duracaoMinutos
				).map(intervalo -> new HorarioComIntervalo(horario, intervalo)))
				.flatMap(Optional::stream)
				.toList();

		if (convertidos.isEmpty()) {
			return new ResultadoHorarios(
					List.of(), SituacaoDisponibilidade.HORARIO_LOCAL_INVALIDO);
		}

		List<HorarioComIntervalo> futuros = convertidos.stream()
				.filter(item -> item.intervalo().inicio().isAfter(agora))
				.toList();

		if (futuros.isEmpty()) {
			return new ResultadoHorarios(
					List.of(), SituacaoDisponibilidade.HORARIOS_ENCERRADOS);
		}

		List<HorarioDisponivelResponse> livres = futuros.stream()
				.filter(item -> ocupados.stream().noneMatch(agendamento ->
								agendamento.getInicio().isBefore(item.intervalo().fim())
								&& agendamento.getFim().isAfter(item.intervalo().inicio())
						))
				.map(HorarioComIntervalo::horario)
				.toList();

		if (livres.isEmpty()) {
			return new ResultadoHorarios(
					List.of(), SituacaoDisponibilidade.HORARIOS_OCUPADOS);
		}

		return new ResultadoHorarios(livres, SituacaoDisponibilidade.DISPONIVEL);
	}

	private Contexto validarContexto(
			Long profissionalId,
			Long servicoId,
			Long servicoAdicionalId
	) {
		Profissional profissional = profissionalService.buscarPorId(profissionalId);

		if (!profissional.isAtivo() || !profissional.getUsuario().isAtivo()) {
			throw new ProfissionalInativoException(profissionalId);
		}
		if (profissional.getBarbearia() != null
				&& !profissional.getBarbearia().isAtivo()) {
			throw new BarbeariaInativaException(profissional.getBarbearia().getId());
		}

		Servico servico = servicoService.buscarPorId(servicoId);

		if (!servico.isAtivo()) {
			throw new ServicoInativoException(servicoId);
		}

		profissionalServicoService.validarAssociacaoAtiva(
				profissionalId,
				servicoId
		);

		int duracaoMinutos = servico.getDuracaoMinutos();
		if (servicoAdicionalId != null) {
			if (servicoId.equals(servicoAdicionalId)) {
				throw new ServicoAdicionalInvalidoException();
			}
			Servico adicional = servicoService.buscarPorId(servicoAdicionalId);
			if (!adicional.isAtivo()) throw new ServicoInativoException(servicoAdicionalId);
			profissionalServicoService.validarAssociacaoAtiva(profissionalId, servicoAdicionalId);
			duracaoMinutos += adicional.getDuracaoMinutos();
		}

		return new Contexto(profissionalId, servicoId, profissional, duracaoMinutos);
	}

	private Optional<IntervaloInstant> converterIntervalo(
			LocalDate data,
			HorarioDisponivelResponse horario,
			ZoneId fusoHorario,
			int duracaoMinutos
	) {
		Optional<Instant> inicio = converterParaInstant(
				data,
				horario.inicio(),
				fusoHorario
		);
		Optional<Instant> fim = converterParaInstant(
				data,
				horario.fim(),
				fusoHorario
		);

		if (inicio.isEmpty()
				|| fim.isEmpty()
				|| Duration.between(inicio.get(), fim.get()).toMinutes()
						!= duracaoMinutos) {
			return Optional.empty();
		}

		return Optional.of(new IntervaloInstant(inicio.get(), fim.get()));
	}

	private Optional<Instant> converterParaInstant(
			LocalDate data,
			LocalTime horario,
			ZoneId fusoHorario
	) {
		LocalDateTime dataHora = LocalDateTime.of(data, horario);
		List<ZoneOffset> offsets = fusoHorario.getRules().getValidOffsets(dataHora);

		if (offsets.size() != 1) {
			return Optional.empty();
		}

		return Optional.of(dataHora.toInstant(offsets.get(0)));
	}

	private DisponibilidadeResponse resposta(
			Long profissionalId,
			Long servicoId,
			LocalDate data,
			SituacaoDisponibilidade situacao,
			List<HorarioDisponivelResponse> horarios
	) {
		return new DisponibilidadeResponse(
				profissionalId,
				servicoId,
				data,
				situacao,
				horarios
		);
	}

	private record Intervalo(LocalTime inicio, LocalTime fim) {
	}

	private record IntervaloInstant(Instant inicio, Instant fim) {
	}

	private record HorarioComIntervalo(
			HorarioDisponivelResponse horario,
			IntervaloInstant intervalo
	) {
	}

	private record ResultadoHorarios(
			List<HorarioDisponivelResponse> horarios,
			SituacaoDisponibilidade situacao
	) {
	}

	private record Contexto(
			Long profissionalId,
			Long servicoId,
			Profissional profissional,
			int duracaoMinutos
	) {
	}
}
