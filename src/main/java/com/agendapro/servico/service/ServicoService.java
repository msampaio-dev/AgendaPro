package com.agendapro.servico.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.servico.entity.Servico;
import com.agendapro.servico.exception.ServicoJaCadastradoException;
import com.agendapro.servico.exception.ServicoNaoEncontradoException;
import com.agendapro.servico.repository.ServicoRepository;

@Service
public class ServicoService {

	private final ServicoRepository servicoRepository;

	public ServicoService(ServicoRepository servicoRepository) {
		this.servicoRepository = servicoRepository;
	}

	@Transactional
	public Servico cadastrar(
			String nome,
			String descricao,
			Integer duracaoMinutos,
			BigDecimal preco
	) {
		String nomeNormalizado = nome.trim();
		String descricaoNormalizada = normalizarDescricao(descricao);

		if (servicoRepository.existsByNomeIgnoreCase(nomeNormalizado)) {
			throw new ServicoJaCadastradoException(nomeNormalizado);
		}

		Servico servico = new Servico(
				nomeNormalizado,
				descricaoNormalizada,
				duracaoMinutos,
				preco
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
		return servicoRepository.findAll();
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
				&& servicoRepository.existsByNomeIgnoreCase(nomeNormalizado)) {
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

	private String normalizarDescricao(String descricao) {
		if (descricao == null || descricao.isBlank()) {
			return null;
		}

		return descricao.trim();
	}
}
