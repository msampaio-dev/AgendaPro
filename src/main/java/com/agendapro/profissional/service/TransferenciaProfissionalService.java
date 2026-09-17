package com.agendapro.profissional.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.agendamento.entity.StatusAgendamento;
import com.agendapro.agendamento.repository.AgendamentoRepository;
import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.barbearia.exception.OperacaoBarbeariaConflitanteException;
import com.agendapro.barbearia.repository.HorarioFuncionamentoBarbeariaRepository;
import com.agendapro.barbearia.repository.BarbeariaRepository;
import com.agendapro.barbearia.service.BarbeariaService;
import com.agendapro.disponibilidade.repository.HorarioAtendimentoRepository;
import com.agendapro.profissional.entity.Profissional;

@Service
public class TransferenciaProfissionalService {
	private final ProfissionalService profissionalService;
	private final BarbeariaService barbeariaService;
	private final BarbeariaRepository barbeariaRepository;
	private final HorarioAtendimentoRepository horarioAtendimentoRepository;
	private final HorarioFuncionamentoBarbeariaRepository funcionamentoRepository;
	private final AgendamentoRepository agendamentoRepository;
	private final Clock clock;

	public TransferenciaProfissionalService(
			ProfissionalService profissionalService,
			BarbeariaService barbeariaService,
			BarbeariaRepository barbeariaRepository,
			HorarioAtendimentoRepository horarioAtendimentoRepository,
			HorarioFuncionamentoBarbeariaRepository funcionamentoRepository,
			AgendamentoRepository agendamentoRepository,
			Clock clock
	) {
		this.profissionalService = profissionalService;
		this.barbeariaService = barbeariaService;
		this.barbeariaRepository = barbeariaRepository;
		this.horarioAtendimentoRepository = horarioAtendimentoRepository;
		this.funcionamentoRepository = funcionamentoRepository;
		this.agendamentoRepository = agendamentoRepository;
		this.clock = clock;
	}

	@Transactional
	public Profissional transferir(Long profissionalId, Long barbeariaId) {
		Profissional profissional = profissionalService.buscarPorId(profissionalId);
		Barbearia destino = barbeariaService.buscarAtivaPorId(barbeariaId);
		if (profissional.getBarbearia().getId().equals(barbeariaId)) return profissional;
		if (barbeariaRepository.existsByProprietarioIdAndAtivoTrue(profissionalId)) {
			throw new OperacaoBarbeariaConflitanteException(
					"O proprietário de uma unidade ativa não pode ser transferido");
		}

		if (agendamentoRepository.existsByProfissionalIdAndStatusInAndInicioAfter(
				profissionalId, StatusAgendamento.QUE_OCUPAM_HORARIO, clock.instant())) {
			throw new OperacaoBarbeariaConflitanteException(
					"O profissional possui agendamentos futuros; conclua ou cancele antes da transferência");
		}

		boolean jornadaFora = horarioAtendimentoRepository
				.findAllByProfissionalIdAndAtivoTrue(profissionalId)
				.stream()
				.anyMatch(jornada -> funcionamentoRepository
						.findAllByBarbeariaIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
								barbeariaId, jornada.getDiaSemana())
						.stream()
						.noneMatch(abertura ->
								!jornada.getHorarioInicio().isBefore(abertura.getHorarioInicio())
										&& !jornada.getHorarioFim().isAfter(abertura.getHorarioFim())));
		if (jornadaFora) {
			throw new OperacaoBarbeariaConflitanteException(
					"A jornada do profissional não cabe no funcionamento da barbearia de destino");
		}

		profissional.transferirPara(destino);
		return profissional;
	}
}
