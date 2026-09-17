package com.agendapro.disponibilidade.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.disponibilidade.entity.HorarioAtendimento;
import com.agendapro.disponibilidade.exception.HorarioAtendimentoInvalidoException;
import com.agendapro.disponibilidade.exception.HorarioAtendimentoNaoEncontradoException;
import com.agendapro.disponibilidade.exception.HorarioAtendimentoSobrepostoException;
import com.agendapro.disponibilidade.repository.HorarioAtendimentoRepository;
import com.agendapro.barbearia.exception.OperacaoBarbeariaConflitanteException;
import com.agendapro.barbearia.repository.HorarioFuncionamentoBarbeariaRepository;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.exception.ProfissionalInativoException;
import com.agendapro.profissional.service.ProfissionalService;

@Service
public class HorarioAtendimentoService {

	private final HorarioAtendimentoRepository horarioAtendimentoRepository;
	private final ProfissionalService profissionalService;
	private final HorarioFuncionamentoBarbeariaRepository funcionamentoRepository;

	public HorarioAtendimentoService(
			HorarioAtendimentoRepository horarioAtendimentoRepository,
			ProfissionalService profissionalService,
			HorarioFuncionamentoBarbeariaRepository funcionamentoRepository
	) {
		this.horarioAtendimentoRepository = horarioAtendimentoRepository;
		this.profissionalService = profissionalService;
		this.funcionamentoRepository = funcionamentoRepository;
	}

	@Transactional
	public HorarioAtendimento cadastrar(
			Long profissionalId,
			DayOfWeek diaSemana,
			LocalTime horarioInicio,
			LocalTime horarioFim
	) {
		validarIntervalo(diaSemana, horarioInicio, horarioFim);

		Profissional profissional = buscarProfissionalAtivo(profissionalId);
		validarDentroDoFuncionamento(profissional, diaSemana, horarioInicio, horarioFim);
		validarSemSobreposicao(profissionalId, diaSemana, horarioInicio, horarioFim);

		HorarioAtendimento horarioAtendimento = new HorarioAtendimento(
				profissional,
				diaSemana,
				horarioInicio,
				horarioFim
		);

		return horarioAtendimentoRepository.save(horarioAtendimento);
	}

	@Transactional
	public List<HorarioAtendimento> cadastrarJornadaComIntervalo(
			Long profissionalId,
			DayOfWeek diaSemana,
			LocalTime horarioInicio,
			LocalTime inicioIntervalo,
			LocalTime fimIntervalo,
			LocalTime horarioFim
	) {
		validarJornadaComIntervalo(
				diaSemana,
				horarioInicio,
				inicioIntervalo,
				fimIntervalo,
				horarioFim
		);

		Profissional profissional = buscarProfissionalAtivo(profissionalId);
		validarDentroDoFuncionamento(profissional, diaSemana, horarioInicio, inicioIntervalo);
		validarDentroDoFuncionamento(profissional, diaSemana, fimIntervalo, horarioFim);
		validarSemSobreposicao(
				profissionalId,
				diaSemana,
				horarioInicio,
				inicioIntervalo
		);
		validarSemSobreposicao(
				profissionalId,
				diaSemana,
				fimIntervalo,
				horarioFim
		);

		List<HorarioAtendimento> periodos = List.of(
				new HorarioAtendimento(
						profissional,
						diaSemana,
						horarioInicio,
						inicioIntervalo
				),
				new HorarioAtendimento(
						profissional,
						diaSemana,
						fimIntervalo,
						horarioFim
				)
		);

		horarioAtendimentoRepository.saveAll(periodos);
		return periodos;
	}

	@Transactional(readOnly = true)
	public HorarioAtendimento buscarPorId(Long id) {
		return horarioAtendimentoRepository.findById(id)
				.orElseThrow(() -> new HorarioAtendimentoNaoEncontradoException(id));
	}

	@Transactional(readOnly = true)
	public List<HorarioAtendimento> listarAtivosPorProfissional(
			Long profissionalId
	) {
		profissionalService.buscarPorId(profissionalId);

		return horarioAtendimentoRepository
				.findAllByProfissionalIdAndAtivoTrue(profissionalId)
				.stream()
				.sorted(
						Comparator
								.comparingInt(
										(HorarioAtendimento horario) ->
											horario.getDiaSemana().getValue()
								)
								.thenComparing(HorarioAtendimento::getHorarioInicio)
				)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<HorarioAtendimento> listarAtivosPorProfissionalEDia(
			Long profissionalId,
			DayOfWeek diaSemana
	) {
		return horarioAtendimentoRepository
				.findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						profissionalId,
						diaSemana
				);
	}

	@Transactional
	public void desativar(Long id) {
		HorarioAtendimento horarioAtendimento = buscarPorId(id);
		horarioAtendimento.desativar();
	}

	private void validarIntervalo(
			DayOfWeek diaSemana,
			LocalTime horarioInicio,
			LocalTime horarioFim
	) {
		if (diaSemana == null
				|| horarioInicio == null
				|| horarioFim == null
				|| !horarioInicio.isBefore(horarioFim)) {
			throw new HorarioAtendimentoInvalidoException();
		}
	}

	private void validarJornadaComIntervalo(
			DayOfWeek diaSemana,
			LocalTime horarioInicio,
			LocalTime inicioIntervalo,
			LocalTime fimIntervalo,
			LocalTime horarioFim
	) {
		if (diaSemana == null
				|| horarioInicio == null
				|| inicioIntervalo == null
				|| fimIntervalo == null
				|| horarioFim == null
				|| !horarioInicio.isBefore(inicioIntervalo)
				|| !inicioIntervalo.isBefore(fimIntervalo)
				|| !fimIntervalo.isBefore(horarioFim)) {
			throw new HorarioAtendimentoInvalidoException(
					"A jornada deve seguir a ordem: início, início do intervalo, fim do intervalo e fim"
			);
		}
	}

	private Profissional buscarProfissionalAtivo(Long profissionalId) {
		Profissional profissional = profissionalService.buscarPorId(profissionalId);

		if (!profissional.isAtivo() || !profissional.getUsuario().isAtivo()) {
			throw new ProfissionalInativoException(profissionalId);
		}

		return profissional;
	}

	private void validarSemSobreposicao(
			Long profissionalId,
			DayOfWeek diaSemana,
			LocalTime horarioInicio,
			LocalTime horarioFim
	) {
		boolean existeSobreposicao = horarioAtendimentoRepository
				.existsByProfissionalIdAndDiaSemanaAndAtivoTrueAndHorarioInicioLessThanAndHorarioFimGreaterThan(
						profissionalId,
						diaSemana,
						horarioFim,
						horarioInicio
				);

		if (existeSobreposicao) {
			throw new HorarioAtendimentoSobrepostoException();
		}
	}

	private void validarDentroDoFuncionamento(
			Profissional profissional,
			DayOfWeek diaSemana,
			LocalTime inicio,
			LocalTime fim
	) {
		if (profissional.getBarbearia() == null || profissional.getBarbearia().getId() == null) return;
		boolean contido = funcionamentoRepository
				.findAllByBarbeariaIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
						profissional.getBarbearia().getId(), diaSemana)
				.stream()
				.anyMatch(abertura -> !inicio.isBefore(abertura.getHorarioInicio())
						&& !fim.isAfter(abertura.getHorarioFim()));
		if (!contido) {
			throw new OperacaoBarbeariaConflitanteException(
					"A jornada do profissional deve estar dentro do funcionamento da barbearia");
		}
	}
}
