package com.agendapro.agendamento.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.agendapro.disponibilidade.service.DisponibilidadeService;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.service.ProfissionalService;
import com.agendapro.servico.entity.Servico;
import com.agendapro.servico.service.ServicoService;
import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.exception.UsuarioInativoException;
import com.agendapro.usuario.service.UsuarioService;

@Service
public class AgendamentoService {
	private static final Set<String> CAMPOS_ORDENACAO_PERMITIDOS = Set.of(
			"id", "inicio", "fim", "status"
	);

	private final AgendamentoRepository agendamentoRepository;
	private final UsuarioService usuarioService;
	private final ProfissionalService profissionalService;
	private final ServicoService servicoService;
	private final DisponibilidadeService disponibilidadeService;
	private final Clock clock;

	public AgendamentoService(
			AgendamentoRepository agendamentoRepository,
			UsuarioService usuarioService,
			ProfissionalService profissionalService,
			ServicoService servicoService,
			DisponibilidadeService disponibilidadeService,
			Clock clock
	) {
		this.agendamentoRepository = agendamentoRepository;
		this.usuarioService = usuarioService;
		this.profissionalService = profissionalService;
		this.servicoService = servicoService;
		this.disponibilidadeService = disponibilidadeService;
		this.clock = clock;
	}

	@Transactional
	public Agendamento agendar(
			Long clienteId,
			Long profissionalId,
			Long servicoId,
			LocalDate data,
			LocalTime horarioInicio
	) {
		Usuario cliente = usuarioService.buscarPorId(clienteId);

		if (!cliente.isAtivo()) {
			throw new UsuarioInativoException(clienteId);
		}

		DisponibilidadeResponse disponibilidade = disponibilidadeService.consultar(
				profissionalId,
				servicoId,
				data
		);

		HorarioDisponivelResponse horario = disponibilidade.horarios()
				.stream()
				.filter(disponivel -> disponivel.inicio().equals(horarioInicio))
				.findFirst()
				.orElseThrow(HorarioIndisponivelException::new);

		Profissional profissional = profissionalService.buscarPorId(profissionalId);
		Servico servico = servicoService.buscarPorId(servicoId);

		Instant inicio = converterParaInstant(
				data,
				horario.inicio(),
				profissional.getFusoHorario()
		);
		Instant fim = converterParaInstant(
				data,
				horario.fim(),
				profissional.getFusoHorario()
		);

		if (!inicio.isAfter(clock.instant())) {
			throw new HorarioIndisponivelException();
		}

		if (Duration.between(inicio, fim).toMinutes()
				!= servico.getDuracaoMinutos()) {
			throw new HorarioLocalInvalidoException();
		}

		boolean sobreposto = agendamentoRepository
				.existsByProfissionalIdAndStatusInAndInicioLessThanAndFimGreaterThan(
						profissionalId,
						StatusAgendamento.QUE_OCUPAM_HORARIO,
						fim,
						inicio
				);

		if (sobreposto) {
			throw new HorarioIndisponivelException();
		}

		Agendamento agendamento = new Agendamento(
				cliente,
				profissional,
				servico,
				inicio,
				fim
		);

		try {
			return agendamentoRepository.saveAndFlush(agendamento);
		} catch (DataIntegrityViolationException exception) {
			throw new HorarioIndisponivelException();
		}
	}

	@Transactional(readOnly = true)
	public Agendamento buscarPorId(Long id) {
		return agendamentoRepository.findById(id)
				.orElseThrow(() -> new AgendamentoNaoEncontradoException(id));
	}

	@Transactional
	public Agendamento confirmar(Long id) {
		Agendamento agendamento = buscarPorId(id);
		agendamento.confirmar();
		return agendamento;
	}

	@Transactional
	public Agendamento cancelar(Long id) {
		Agendamento agendamento = buscarPorId(id);
		agendamento.cancelar();
		return agendamento;
	}

	@Transactional
	public Agendamento concluir(Long id) {
		Agendamento agendamento = buscarPorId(id);
		if (agendamento.getFim().isAfter(clock.instant())) {
			throw new AtendimentoAindaNaoFinalizadoException();
		}
		agendamento.concluir();
		return agendamento;
	}

	@Transactional(readOnly = true)
	public Page<Agendamento> listarPorCliente(
			Long clienteId,
			OffsetDateTime inicioDe,
			OffsetDateTime inicioAntesDe,
			StatusAgendamento status,
			Pageable pageable
	) {
		usuarioService.buscarPorId(clienteId);

		if (inicioDe != null && inicioAntesDe != null
				&& !inicioDe.isBefore(inicioAntesDe)) {
			throw new FiltroAgendamentoInvalidoException(
					"inicioDe deve ser anterior a inicioAntesDe"
			);
		}

		Specification<Agendamento> filtro = (root, query, builder) ->
				builder.equal(root.get("cliente").get("id"), clienteId);

		if (status != null) {
			filtro = filtro.and((root, query, builder) ->
					builder.equal(root.get("status"), status));
		}
		if (inicioDe != null) {
			Instant inicio = inicioDe.toInstant();
			filtro = filtro.and((root, query, builder) ->
					builder.greaterThanOrEqualTo(root.get("inicio"), inicio));
		}
		if (inicioAntesDe != null) {
			Instant fim = inicioAntesDe.toInstant();
			filtro = filtro.and((root, query, builder) ->
					builder.lessThan(root.get("inicio"), fim));
		}

		return agendamentoRepository.findAll(filtro, normalizarPaginacao(pageable));
	}

	@Transactional(readOnly = true)
	public Page<Agendamento> listarPorProfissional(
			Long profissionalId,
			LocalDate data,
			LocalDate dataInicio,
			LocalDate dataFim,
			StatusAgendamento status,
			Pageable pageable
	) {
		if (data != null && (dataInicio != null || dataFim != null)) {
			throw new FiltroAgendamentoInvalidoException(
					"use 'data' ou o intervalo 'dataInicio/dataFim', não os dois"
			);
		}

		if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
			throw new FiltroAgendamentoInvalidoException(
					"dataInicio não pode ser posterior a dataFim"
			);
		}

		Profissional profissional = profissionalService.buscarPorId(profissionalId);
		ZoneId fusoHorario = profissional.getFusoHorario();
		LocalDate inicioLocal = data != null ? data : dataInicio;
		LocalDate fimLocal = data != null ? data : dataFim;
		Instant inicio = inicioLocal == null
				? null
				: inicioLocal.atStartOfDay(fusoHorario).toInstant();
		Instant fimExclusivo = fimLocal == null
				? null
				: fimLocal.plusDays(1).atStartOfDay(fusoHorario).toInstant();

		Specification<Agendamento> filtro = (root, query, builder) ->
				builder.equal(root.get("profissional").get("id"), profissionalId);

		if (status != null) {
			filtro = filtro.and((root, query, builder) ->
					builder.equal(root.get("status"), status));
		}
		if (inicio != null) {
			filtro = filtro.and((root, query, builder) ->
					builder.greaterThanOrEqualTo(root.get("inicio"), inicio));
		}
		if (fimExclusivo != null) {
			filtro = filtro.and((root, query, builder) ->
					builder.lessThan(root.get("inicio"), fimExclusivo));
		}

		return agendamentoRepository.findAll(filtro, normalizarPaginacao(pageable));
	}

	@Transactional(readOnly = true)
	public List<Agendamento> listarPorProfissionalEData(
			Long profissionalId,
			LocalDate data
	) {
		Profissional profissional = profissionalService.buscarPorId(profissionalId);
		ZoneId fusoHorario = profissional.getFusoHorario();
		Instant inicioDoDia = data.atStartOfDay(fusoHorario).toInstant();
		Instant inicioDoProximoDia = data
				.plusDays(1)
				.atStartOfDay(fusoHorario)
				.toInstant();

		return agendamentoRepository
				.findAllByProfissionalIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicio(
						profissionalId,
						inicioDoDia,
						inicioDoProximoDia
				);
	}

	private Pageable normalizarPaginacao(Pageable pageable) {
		Sort ordenacao = pageable.getSort().isUnsorted()
				? Sort.by(Sort.Direction.DESC, "inicio")
				: pageable.getSort();

		for (Sort.Order ordem : ordenacao) {
			if (!CAMPOS_ORDENACAO_PERMITIDOS.contains(ordem.getProperty())) {
				throw new FiltroAgendamentoInvalidoException(
						"ordenação permitida apenas por id, inicio, fim ou status"
				);
			}
		}

		return PageRequest.of(
				pageable.getPageNumber(),
				Math.min(pageable.getPageSize(), 100),
				ordenacao
		);
	}

	private Instant converterParaInstant(
			LocalDate data,
			LocalTime horario,
			ZoneId fusoHorario
	) {
		LocalDateTime dataHora = LocalDateTime.of(data, horario);
		List<ZoneOffset> offsetsValidos = fusoHorario
				.getRules()
				.getValidOffsets(dataHora);

		if (offsetsValidos.size() != 1) {
			throw new HorarioLocalInvalidoException();
		}

		return dataHora.toInstant(offsetsValidos.get(0));
	}
}
