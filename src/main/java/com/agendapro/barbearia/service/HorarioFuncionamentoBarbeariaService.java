package com.agendapro.barbearia.service;

import java.time.Clock;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.agendamento.entity.StatusAgendamento;
import com.agendapro.agendamento.repository.AgendamentoRepository;
import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.barbearia.entity.HorarioFuncionamentoBarbearia;
import com.agendapro.barbearia.exception.HorarioFuncionamentoInvalidoException;
import com.agendapro.barbearia.exception.HorarioFuncionamentoNaoEncontradoException;
import com.agendapro.barbearia.exception.HorarioFuncionamentoSobrepostoException;
import com.agendapro.barbearia.exception.OperacaoBarbeariaConflitanteException;
import com.agendapro.barbearia.repository.HorarioFuncionamentoBarbeariaRepository;
import com.agendapro.disponibilidade.repository.HorarioAtendimentoRepository;

@Service
public class HorarioFuncionamentoBarbeariaService {
	private final HorarioFuncionamentoBarbeariaRepository repository;
	private final BarbeariaService barbeariaService;
	private final AgendamentoRepository agendamentoRepository;
	private final HorarioAtendimentoRepository horarioAtendimentoRepository;
	private final Clock clock;

	public HorarioFuncionamentoBarbeariaService(
			HorarioFuncionamentoBarbeariaRepository repository,
			BarbeariaService barbeariaService,
			AgendamentoRepository agendamentoRepository,
			HorarioAtendimentoRepository horarioAtendimentoRepository,
			Clock clock
	) {
		this.repository = repository;
		this.barbeariaService = barbeariaService;
		this.agendamentoRepository = agendamentoRepository;
		this.horarioAtendimentoRepository = horarioAtendimentoRepository;
		this.clock = clock;
	}

	@Transactional
	public HorarioFuncionamentoBarbearia cadastrar(
			Long barbeariaId,
			java.time.DayOfWeek diaSemana,
			LocalTime inicio,
			LocalTime fim
	) {
		validarIntervalo(diaSemana, inicio, fim);
		Barbearia barbearia = barbeariaService.buscarAtivaPorId(barbeariaId);
		if (repository.existsByBarbeariaIdAndDiaSemanaAndAtivoTrueAndHorarioInicioLessThanAndHorarioFimGreaterThan(
				barbeariaId, diaSemana, fim, inicio)) {
			throw new HorarioFuncionamentoSobrepostoException();
		}
		return repository.save(new HorarioFuncionamentoBarbearia(barbearia, diaSemana, inicio, fim));
	}

	@Transactional(readOnly = true)
	public List<HorarioFuncionamentoBarbearia> listar(Long barbeariaId) {
		barbeariaService.buscarPorId(barbeariaId);
		return repository.findAllByBarbeariaIdAndAtivoTrueOrderByDiaSemanaAscHorarioInicioAsc(barbeariaId);
	}

	@Transactional
	public void desativar(Long id) {
		HorarioFuncionamentoBarbearia horario = repository.findById(id)
				.orElseThrow(() -> new HorarioFuncionamentoNaoEncontradoException(id));
		validarSemAgendamentosFuturos(horario);
		horario.desativar();
	}

	private void validarSemAgendamentosFuturos(HorarioFuncionamentoBarbearia horario) {
		Barbearia barbearia = horario.getBarbearia();
		boolean existeJornadaAfetada = horarioAtendimentoRepository
				.findAllByProfissionalBarbeariaIdAndDiaSemanaAndAtivoTrue(
						barbearia.getId(), horario.getDiaSemana())
				.stream()
				.anyMatch(jornada -> jornada.getHorarioInicio().isBefore(horario.getHorarioFim())
						&& jornada.getHorarioFim().isAfter(horario.getHorarioInicio()));
		if (existeJornadaAfetada) {
			throw new OperacaoBarbeariaConflitanteException(
					"O horário ainda contém jornadas de profissionais e não pode ser removido"
			);
		}
		boolean existeAfetado = agendamentoRepository
				.findAllByBarbeariaIdAndStatusInAndInicioAfterOrderByInicio(
						barbearia.getId(), StatusAgendamento.QUE_OCUPAM_HORARIO, clock.instant())
				.stream()
				.anyMatch(agendamento -> {
					var inicio = agendamento.getInicio().atZone(barbearia.getFusoHorario());
					var fim = agendamento.getFim().atZone(barbearia.getFusoHorario());
					return inicio.getDayOfWeek() == horario.getDiaSemana()
							&& !inicio.toLocalTime().isBefore(horario.getHorarioInicio())
							&& !fim.toLocalTime().isAfter(horario.getHorarioFim());
				});
		if (existeAfetado) {
			throw new OperacaoBarbeariaConflitanteException(
					"O horário possui agendamentos futuros e não pode ser removido"
			);
		}
	}

	private void validarIntervalo(
			java.time.DayOfWeek diaSemana,
			LocalTime inicio,
			LocalTime fim
	) {
		if (diaSemana == null || inicio == null || fim == null || !inicio.isBefore(fim)) {
			throw new HorarioFuncionamentoInvalidoException(
					"O início do funcionamento deve ser anterior ao fim"
			);
		}
	}
}
