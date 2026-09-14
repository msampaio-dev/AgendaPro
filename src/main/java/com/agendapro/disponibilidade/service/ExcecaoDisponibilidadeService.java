package com.agendapro.disponibilidade.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.disponibilidade.entity.ExcecaoDisponibilidade;
import com.agendapro.disponibilidade.entity.TipoExcecaoDisponibilidade;
import com.agendapro.disponibilidade.exception.ExcecaoDisponibilidadeInvalidaException;
import com.agendapro.disponibilidade.exception.ExcecaoDisponibilidadeJaCadastradaException;
import com.agendapro.disponibilidade.exception.ExcecaoDisponibilidadeNaoEncontradaException;
import com.agendapro.disponibilidade.repository.ExcecaoDisponibilidadeRepository;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.exception.ProfissionalInativoException;
import com.agendapro.profissional.service.ProfissionalService;

@Service
public class ExcecaoDisponibilidadeService {

	private final ExcecaoDisponibilidadeRepository excecaoRepository;
	private final ProfissionalService profissionalService;

	public ExcecaoDisponibilidadeService(
			ExcecaoDisponibilidadeRepository excecaoRepository,
			ProfissionalService profissionalService
	) {
		this.excecaoRepository = excecaoRepository;
		this.profissionalService = profissionalService;
	}

	@Transactional
	public ExcecaoDisponibilidade cadastrar(
			Long profissionalId,
			LocalDate data,
			TipoExcecaoDisponibilidade tipo,
			LocalTime horarioInicio,
			LocalTime horarioFim
	) {
		validar(data, tipo, horarioInicio, horarioFim);

		Profissional profissional = profissionalService.buscarPorId(profissionalId);

		if (!profissional.isAtivo() || !profissional.getUsuario().isAtivo()) {
			throw new ProfissionalInativoException(profissionalId);
		}

		boolean duplicada = excecaoRepository
				.existsByProfissionalIdAndDataAndTipoAndHorarioInicioAndHorarioFimAndAtivoTrue(
						profissionalId,
						data,
						tipo,
						horarioInicio,
						horarioFim
				);

		if (duplicada) {
			throw new ExcecaoDisponibilidadeJaCadastradaException();
		}

		ExcecaoDisponibilidade excecao = new ExcecaoDisponibilidade(
				profissional,
				data,
				tipo,
				horarioInicio,
				horarioFim
		);

		return excecaoRepository.save(excecao);
	}

	@Transactional(readOnly = true)
	public ExcecaoDisponibilidade buscarPorId(Long id) {
		return excecaoRepository.findById(id)
				.orElseThrow(
						() -> new ExcecaoDisponibilidadeNaoEncontradaException(id)
				);
	}

	@Transactional(readOnly = true)
	public List<ExcecaoDisponibilidade> listarAtivasPorProfissionalEData(
			Long profissionalId,
			LocalDate data
	) {
		profissionalService.buscarPorId(profissionalId);

		return excecaoRepository
				.findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
						profissionalId,
						data
				);
	}

	@Transactional
	public void desativar(Long id) {
		ExcecaoDisponibilidade excecao = buscarPorId(id);
		excecao.desativar();
	}

	private void validar(
			LocalDate data,
			TipoExcecaoDisponibilidade tipo,
			LocalTime horarioInicio,
			LocalTime horarioFim
	) {
		if (data == null || tipo == null) {
			throw new ExcecaoDisponibilidadeInvalidaException(
					"data e tipo são obrigatórios"
			);
		}

		boolean ambosAusentes = horarioInicio == null && horarioFim == null;
		boolean ambosPresentes = horarioInicio != null && horarioFim != null;

		if (!ambosAusentes && !ambosPresentes) {
			throw new ExcecaoDisponibilidadeInvalidaException(
					"início e fim devem ser informados juntos"
			);
		}

		if (ambosPresentes && !horarioInicio.isBefore(horarioFim)) {
			throw new ExcecaoDisponibilidadeInvalidaException(
					"o início deve ser anterior ao fim"
			);
		}

		if (tipo == TipoExcecaoDisponibilidade.DISPONIBILIDADE_EXTRA
				&& ambosAusentes) {
			throw new ExcecaoDisponibilidadeInvalidaException(
					"disponibilidade extra exige início e fim"
			);
		}
	}
}
