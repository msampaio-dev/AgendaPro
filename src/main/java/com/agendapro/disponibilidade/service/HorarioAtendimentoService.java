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
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.exception.ProfissionalInativoException;
import com.agendapro.profissional.service.ProfissionalService;

@Service
public class HorarioAtendimentoService {

	private final HorarioAtendimentoRepository horarioAtendimentoRepository;
	private final ProfissionalService profissionalService;

	public HorarioAtendimentoService(
			HorarioAtendimentoRepository horarioAtendimentoRepository,
			ProfissionalService profissionalService
	) {
		this.horarioAtendimentoRepository = horarioAtendimentoRepository;
		this.profissionalService = profissionalService;
	}

	@Transactional
	public HorarioAtendimento cadastrar(
			Long profissionalId,
			DayOfWeek diaSemana,
			LocalTime horarioInicio,
			LocalTime horarioFim
	) {
		validarIntervalo(diaSemana, horarioInicio, horarioFim);

		Profissional profissional = profissionalService.buscarPorId(profissionalId);

		if (!profissional.isAtivo() || !profissional.getUsuario().isAtivo()) {
			throw new ProfissionalInativoException(profissionalId);
		}

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

		HorarioAtendimento horarioAtendimento = new HorarioAtendimento(
				profissional,
				diaSemana,
				horarioInicio,
				horarioFim
		);

		return horarioAtendimentoRepository.save(horarioAtendimento);
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
}
