package com.agendapro.servico.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.barbearia.exception.BarbeariaInativaException;
import com.agendapro.barbearia.exception.BarbeariaNaoEncontradaException;
import com.agendapro.barbearia.repository.BarbeariaRepository;
import com.agendapro.servico.entity.Servico;
import com.agendapro.servico.exception.ServicoJaCadastradoException;
import com.agendapro.servico.exception.ServicoNaoEncontradoException;
import com.agendapro.servico.repository.ServicoRepository;

@Service
public class ServicoService {

	private final ServicoRepository servicoRepository;
	private final BarbeariaRepository barbeariaRepository;

	public ServicoService(
			ServicoRepository servicoRepository,
			BarbeariaRepository barbeariaRepository
	) {
		this.servicoRepository = servicoRepository;
		this.barbeariaRepository = barbeariaRepository;
	}

	@Transactional
	public Servico cadastrar(
			Long barbeariaId,
			String nome,
			String descricao,
			Integer duracaoMinutos,
			BigDecimal preco
	) {
		Barbearia barbearia = buscarBarbeariaAtiva(barbeariaId);
		String nomeNormalizado = nome.trim();
		String descricaoNormalizada = normalizarDescricao(descricao);

		// O nome e unico dentro da barbearia: duas barbearias podem oferecer
		// "Barba" com duracoes e precos proprios.
		if (servicoRepository.existsByBarbeariaIdAndNomeIgnoreCase(
				barbeariaId,
				nomeNormalizado
		)) {
			throw new ServicoJaCadastradoException(nomeNormalizado);
		}

		Servico servico = new Servico(
				nomeNormalizado,
				descricaoNormalizada,
				duracaoMinutos,
				preco,
				barbearia
		);

		return servicoRepository.save(servico);
	}

	@Transactional(readOnly = true)
	public Servico buscarPorId(Long id) {
		return servicoRepository.findById(id)
				.orElseThrow(() -> new ServicoNaoEncontradoException(id));
	}

	@Transactional(readOnly = true)
	public List<Servico> listar() {
		return servicoRepository.findAllByOrderByNomeAsc();
	}

	@Transactional(readOnly = true)
	public List<Servico> listarPorBarbearia(Long barbeariaId) {
		garantirBarbeariaExistente(barbeariaId);
		return servicoRepository.findAllByBarbeariaIdOrderByNomeAsc(barbeariaId);
	}

	@Transactional(readOnly = true)
	public List<Servico> listarAtivosPorBarbearia(Long barbeariaId) {
		garantirBarbeariaExistente(barbeariaId);
		return servicoRepository
				.findAllByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbeariaId);
	}

	@Transactional
	public Servico atualizar(
			Long id,
			String nome,
			String descricao,
			Integer duracaoMinutos,
			BigDecimal preco
	) {
		Servico servico = buscarPorId(id);
		String nomeNormalizado = nome.trim();
		String descricaoNormalizada = normalizarDescricao(descricao);

		boolean nomeFoiAlterado =
				!servico.getNome().equalsIgnoreCase(nomeNormalizado);

		if (nomeFoiAlterado
				&& servicoRepository.existsByBarbeariaIdAndNomeIgnoreCase(
						servico.getBarbearia().getId(),
						nomeNormalizado
				)) {
			throw new ServicoJaCadastradoException(nomeNormalizado);
		}

		servico.atualizarDados(
				nomeNormalizado,
				descricaoNormalizada,
				duracaoMinutos,
				preco
		);

		return servico;
	}

	@Transactional
	public void desativar(Long id) {
		Servico servico = buscarPorId(id);
		servico.desativar();
	}

	private Barbearia buscarBarbeariaAtiva(Long barbeariaId) {
		Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
				.orElseThrow(() -> new BarbeariaNaoEncontradaException(barbeariaId));

		if (!barbearia.isAtivo()) {
			throw new BarbeariaInativaException(barbeariaId);
		}

		return barbearia;
	}

	private void garantirBarbeariaExistente(Long barbeariaId) {
		if (!barbeariaRepository.existsById(barbeariaId)) {
			throw new BarbeariaNaoEncontradaException(barbeariaId);
		}
	}

	private String normalizarDescricao(String descricao) {
		if (descricao == null || descricao.isBlank()) {
			return null;
		}

		return descricao.trim();
	}
}
