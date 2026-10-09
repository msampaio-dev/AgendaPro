package com.agendapro.sugestao.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.barbearia.repository.BarbeariaRepository;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.repository.ProfissionalRepository;
import com.agendapro.profissionalservico.repository.ProfissionalServicoRepository;
import com.agendapro.servico.repository.ServicoRepository;
import com.agendapro.sugestao.ia.CatalogoSugestao;

/**
 * Monta a lista fechada do que pode ser sugerido: so barbearias, profissionais
 * e servicos ativos, com quem faz o que.
 *
 * Sao tres consultas por barbearia: servicos, equipe e associacoes. Com o
 * catalogo da demonstracao (tres barbearias) isso nao pesa; com centenas
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
	 *
	 * O corte acontece no SQL (findTop30...): cortar em memoria leria do banco
	 * todos os servicos de uma unidade inflada a cada sugestao. Se este valor
	 * mudar, o nome das consultas precisa mudar junto.
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
				.findTop30ByBarbeariaIdAndAtivoTrueOrderByIdAsc(barbearia.getId())
				.stream()
				.map(servico -> new CatalogoSugestao.Servico(
						servico.getId(), servico.getNome(), servico.getDuracaoMinutos()))
				.toList();
		Set<Long> servicoIds = servicos.stream()
				.map(CatalogoSugestao.Servico::id)
				.collect(Collectors.toSet());

		List<Profissional> equipe = profissionalRepository
				.findTop30ByBarbeariaIdAndAtivoTrueAndUsuarioAtivoTrueOrderByIdAsc(barbearia.getId());
		Map<Long, List<Long>> servicosPorProfissional = servicosPorProfissional(equipe, servicoIds);

		List<CatalogoSugestao.Profissional> profissionais = equipe.stream()
				.map(profissional -> new CatalogoSugestao.Profissional(
						profissional.getId(),
						profissional.getUsuario().getNome(),
						servicosPorProfissional.getOrDefault(profissional.getId(), List.of())))
				.toList();

		return new CatalogoSugestao.Barbearia(
				barbearia.getId(), barbearia.getNome(), profissionais, servicos);
	}

	/** Uma consulta por barbearia, em vez de uma por profissional. */
	private Map<Long, List<Long>> servicosPorProfissional(
			List<Profissional> equipe,
			Set<Long> servicosDoCatalogo
	) {
		if (equipe.isEmpty() || servicosDoCatalogo.isEmpty()) {
			return Map.of();
		}
		List<Long> profissionalIds = equipe.stream().map(Profissional::getId).toList();
		return profissionalServicoRepository
				.findAllByProfissionalIdInAndServicoIdInAndAtivoTrue(profissionalIds, servicosDoCatalogo)
				.stream()
				.collect(Collectors.groupingBy(
						associacao -> associacao.getProfissional().getId(),
						Collectors.mapping(
								associacao -> associacao.getServico().getId(),
								Collectors.collectingAndThen(Collectors.toList(),
										ids -> ids.stream().sorted().toList()))));
	}
}
