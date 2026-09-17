package com.agendapro.profissionalservico.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.exception.ProfissionalInativoException;
import com.agendapro.profissional.service.ProfissionalService;
import com.agendapro.profissionalservico.entity.ProfissionalServico;
import com.agendapro.profissionalservico.exception.ProfissionalServicoJaCadastradoException;
import com.agendapro.profissionalservico.exception.ProfissionalServicoNaoEncontradoException;
import com.agendapro.profissionalservico.exception.ProfissionalNaoRealizaServicoException;
import com.agendapro.profissionalservico.repository.ProfissionalServicoRepository;
import com.agendapro.servico.entity.Servico;
import com.agendapro.servico.exception.ServicoInativoException;
import com.agendapro.servico.service.ServicoService;

@Service
public class ProfissionalServicoService {

	private final ProfissionalServicoRepository profissionalServicoRepository;
	private final ProfissionalService profissionalService;
	private final ServicoService servicoService;

	public ProfissionalServicoService(
			ProfissionalServicoRepository profissionalServicoRepository,
			ProfissionalService profissionalService,
			ServicoService servicoService
	) {
		this.profissionalServicoRepository = profissionalServicoRepository;
		this.profissionalService = profissionalService;
		this.servicoService = servicoService;
	}

	@Transactional
	public ProfissionalServico associar(Long profissionalId, Long servicoId) {
		Profissional profissional = profissionalService.buscarPorId(profissionalId);

		if (!profissional.isAtivo()) {
			throw new ProfissionalInativoException(profissionalId);
		}

		Servico servico = servicoService.buscarPorId(servicoId);

		if (!servico.isAtivo()) {
			throw new ServicoInativoException(servicoId);
		}

		var associacaoExistente = profissionalServicoRepository
				.findByProfissionalIdAndServicoId(profissionalId, servicoId);

		if (associacaoExistente.isPresent()) {
			ProfissionalServico profissionalServico = associacaoExistente.get();

			if (profissionalServico.isAtivo()) {
				throw new ProfissionalServicoJaCadastradoException(
						profissionalId,
						servicoId
				);
			}

			profissionalServico.ativar();
			return profissionalServico;
		}

		ProfissionalServico profissionalServico =
				new ProfissionalServico(profissional, servico);

		return profissionalServicoRepository.save(profissionalServico);
	}

	@Transactional(readOnly = true)
	public ProfissionalServico buscarPorId(Long id) {
		return profissionalServicoRepository.findById(id)
				.orElseThrow(
						() -> new ProfissionalServicoNaoEncontradoException(id)
				);
	}

	@Transactional(readOnly = true)
	public List<ProfissionalServico> listarAtivosPorProfissional(
			Long profissionalId
	) {
		profissionalService.buscarPorId(profissionalId);

		return profissionalServicoRepository
				.findAllByProfissionalIdAndAtivoTrue(profissionalId);
	}

	@Transactional(readOnly = true)
	public List<ProfissionalServico> listarAtivosPorServico(Long servicoId) {
		servicoService.buscarPorId(servicoId);

		return profissionalServicoRepository
				.findAllByServicoIdAndAtivoTrue(servicoId);
	}

	@Transactional(readOnly = true)
	public void validarAssociacaoAtiva(Long profissionalId, Long servicoId) {
		boolean associacaoAtiva = profissionalServicoRepository
				.existsByProfissionalIdAndServicoIdAndAtivoTrue(
						profissionalId,
						servicoId
				);

		if (!associacaoAtiva) {
			throw new ProfissionalNaoRealizaServicoException(
					profissionalId,
					servicoId
			);
		}
	}

	@Transactional
	public void desativar(Long id) {
		ProfissionalServico profissionalServico = buscarPorId(id);
		profissionalServico.desativar();
	}
}
