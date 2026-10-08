package com.agendapro.sugestao.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.barbearia.repository.BarbeariaRepository;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.repository.ProfissionalRepository;
import com.agendapro.profissionalservico.repository.ProfissionalServicoRepository;
import com.agendapro.servico.entity.Servico;
import com.agendapro.servico.repository.ServicoRepository;
import com.agendapro.sugestao.ia.CatalogoSugestao;

/**
 * Monta a lista fechada do que pode ser sugerido: so barbearias, profissionais
 * e servicos ativos, com quem faz o que.
 *
 * As consultas sao uma por barbearia e por profissional. Com o catalogo da
 * demonstracao (tres barbearias, seis profissionais) isso nao pesa; com centenas
 * de unidades o catalogo inteiro nem caberia no prompt e o desenho teria que
 * mudar (filtrar pela cidade do cliente, ou deixar o modelo buscar por
 * ferramenta).
 */
@Service
public class CatalogoSugestaoService {

	/**
	 * Teto por barbearia. Quem administra uma unidade cria servicos a vontade (a
	 * conta de demonstracao e publica e e de profissional), e cada servico vira
	 * texto pago em toda chamada a IA. Ficam os mais antigos, para que servicos
	 * criados em massa nao tirem do prompt os que ja existiam.
	 */
	static final int MAXIMO_POR_BARBEARIA = 30;

	private final BarbeariaRepository barbeariaRepository;
	private final ProfissionalRepository profissionalRepository;
	private final ServicoRepository servicoRepository;
	private final ProfissionalServicoRepository profissionalServicoRepository;

	public CatalogoSugestaoService(
			BarbeariaRepository barbeariaRepository,
			ProfissionalRepository profissionalRepository,
			ServicoRepository servicoRepository,
			ProfissionalServicoRepository profissionalServicoRepository
	) {
		this.barbeariaRepository = barbeariaRepository;
		this.profissionalRepository = profissionalRepository;
		this.servicoRepository = servicoRepository;
		this.profissionalServicoRepository = profissionalServicoRepository;
	}

	@Transactional(readOnly = true)
	public CatalogoSugestao montar(LocalDate hoje) {
		List<CatalogoSugestao.Barbearia> barbearias = barbeariaRepository
				.findAllByAtivoTrueAndProprietarioIsNotNullOrderByNomeAsc()
				.stream()
				.map(this::barbearia)
				.toList();
		return new CatalogoSugestao(hoje, barbearias);
	}

	private CatalogoSugestao.Barbearia barbearia(Barbearia barbearia) {
		List<CatalogoSugestao.Servico> servicos = servicoRepository
				.findAllByBarbeariaIdAndAtivoTrueOrderByNomeAsc(barbearia.getId())
				.stream()
				.sorted(Comparator.comparing(Servico::getId))
				.limit(MAXIMO_POR_BARBEARIA)
				.map(servico -> new CatalogoSugestao.Servico(
						servico.getId(), servico.getNome(), servico.getDuracaoMinutos()))
				.toList();

		List<CatalogoSugestao.Profissional> profissionais = profissionalRepository
				.findAllByBarbeariaId(barbearia.getId())
				.stream()
				.filter(profissional -> profissional.isAtivo() && profissional.getUsuario().isAtivo())
				.sorted(Comparator.comparing(Profissional::getId))
				.limit(MAXIMO_POR_BARBEARIA)
				.map(this::profissional)
				.toList();

		return new CatalogoSugestao.Barbearia(
				barbearia.getId(), barbearia.getNome(), profissionais, servicos);
	}

	private CatalogoSugestao.Profissional profissional(Profissional profissional) {
		List<Long> servicoIds = profissionalServicoRepository
				.findAllByProfissionalIdAndAtivoTrue(profissional.getId())
				.stream()
				.filter(associacao -> associacao.getServico().isAtivo())
				.map(associacao -> associacao.getServico().getId())
				.toList();
		return new CatalogoSugestao.Profissional(
				profissional.getId(), profissional.getUsuario().getNome(), servicoIds);
	}
}
