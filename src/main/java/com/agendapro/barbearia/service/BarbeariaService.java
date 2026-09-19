package com.agendapro.barbearia.service;

import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.agendamento.entity.StatusAgendamento;
import com.agendapro.agendamento.repository.AgendamentoRepository;
import com.agendapro.barbearia.dto.AtualizacaoBarbeariaRequest;
import com.agendapro.barbearia.dto.CadastroBarbeariaAdminRequest;
import com.agendapro.barbearia.dto.CadastroBarbeariaRequest;
import com.agendapro.barbearia.dto.HorarioFuncionamentoRequest;
import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.barbearia.entity.EnderecoBarbearia;
import com.agendapro.barbearia.entity.HorarioFuncionamentoBarbearia;
import com.agendapro.barbearia.exception.BarbeariaInativaException;
import com.agendapro.barbearia.exception.BarbeariaJaCadastradaException;
import com.agendapro.barbearia.exception.BarbeariaNaoEncontradaException;
import com.agendapro.barbearia.exception.HorarioFuncionamentoInvalidoException;
import com.agendapro.barbearia.exception.HorarioFuncionamentoSobrepostoException;
import com.agendapro.barbearia.exception.OperacaoBarbeariaConflitanteException;
import com.agendapro.barbearia.repository.BarbeariaRepository;
import com.agendapro.barbearia.repository.HorarioFuncionamentoBarbeariaRepository;
import com.agendapro.disponibilidade.repository.HorarioAtendimentoRepository;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.exception.ProfissionalInativoException;
import com.agendapro.profissional.exception.ProfissionalNaoEncontradoException;
import com.agendapro.profissional.repository.ProfissionalRepository;

@Service
public class BarbeariaService {
	private final BarbeariaRepository repository;
	private final ProfissionalRepository profissionalRepository;
	private final HorarioFuncionamentoBarbeariaRepository horarioFuncionamentoRepository;
	private final HorarioAtendimentoRepository horarioAtendimentoRepository;
	private final AgendamentoRepository agendamentoRepository;
	private final Clock clock;

	public BarbeariaService(
			BarbeariaRepository repository,
			ProfissionalRepository profissionalRepository,
			HorarioFuncionamentoBarbeariaRepository horarioFuncionamentoRepository,
			HorarioAtendimentoRepository horarioAtendimentoRepository,
			AgendamentoRepository agendamentoRepository,
			Clock clock
	) {
		this.repository = repository;
		this.profissionalRepository = profissionalRepository;
		this.horarioFuncionamentoRepository = horarioFuncionamentoRepository;
		this.horarioAtendimentoRepository = horarioAtendimentoRepository;
		this.agendamentoRepository = agendamentoRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Barbearia buscarPorId(Long id) {
		return repository.findById(id).orElseThrow(() -> new BarbeariaNaoEncontradaException(id));
	}

	@Transactional(readOnly = true)
	public Barbearia buscarAtivaPorId(Long id) {
		Barbearia barbearia = buscarPorId(id);
		if (!barbearia.isAtivo()) throw new BarbeariaInativaException(id);
		return barbearia;
	}

	@Transactional(readOnly = true)
	public List<Barbearia> listar() {
		return repository.findAll();
	}

	@Transactional(readOnly = true)
	public List<Barbearia> listarAtivas() {
		return repository.findAllByAtivoTrueAndProprietarioIsNotNullOrderByNomeAsc();
	}

	/**
	 * As unidades que o profissional administra: as que possui e tambem aquela
	 * onde trabalha como membro da equipe. O contratado nao e dono, mas cuida do
	 * proprio catalogo, entao a barbearia dele precisa aparecer aqui.
	 */
	@Transactional(readOnly = true)
	public List<Barbearia> listarDoProfissional(Long usuarioId) {
		List<Barbearia> unidades =
				new ArrayList<>(repository.findAllByProprietarioUsuarioId(usuarioId));

		// Recarregada por findById de proposito: o grafo de entidades de la traz
		// o proprietario, que a resposta precisa e que a sessao nao abriria de
		// novo com open-in-view desligado.
		profissionalRepository.findByUsuarioIdAndAtivoTrue(usuarioId)
				.map(Profissional::getBarbearia)
				.map(Barbearia::getId)
				.filter(id -> unidades.stream()
						.noneMatch(unidade -> unidade.getId().equals(id)))
				.flatMap(repository::findById)
				.ifPresent(unidades::add);

		return unidades;
	}

	@Transactional
	public Barbearia cadastrarPeloProfissional(Long usuarioId, CadastroBarbeariaRequest request) {
		Profissional proprietario = profissionalRepository.findByUsuarioIdAndAtivoTrue(usuarioId)
				.orElseThrow(() -> new ProfissionalNaoEncontradoException(usuarioId));
		return cadastrar(proprietario, request.nome(), request.endereco().toEntity(), request.horarios());
	}

	@Transactional
	public Barbearia cadastrarPeloAdmin(CadastroBarbeariaAdminRequest request) {
		Profissional proprietario = profissionalRepository.findById(request.proprietarioProfissionalId())
				.orElseThrow(() -> new ProfissionalNaoEncontradoException(request.proprietarioProfissionalId()));
		return cadastrar(proprietario, request.nome(), request.endereco().toEntity(), request.horarios());
	}

	@Transactional
	public Barbearia atualizar(Long id, AtualizacaoBarbeariaRequest request) {
		Barbearia barbearia = buscarPorId(id);
		String nome = normalizarNome(request.nome());
		validarNomeDisponivel(nome, barbearia);
		barbearia.atualizarDados(nome, request.endereco().toEntity(), barbearia.getFusoHorario());
		return barbearia;
	}

	@Transactional
	public Barbearia alterarProprietario(Long id, Long profissionalId) {
		Barbearia barbearia = buscarAtivaPorId(id);
		Profissional novoProprietario = profissionalRepository.findById(profissionalId)
				.orElseThrow(() -> new ProfissionalNaoEncontradoException(profissionalId));
		if (!novoProprietario.isAtivo()
				|| novoProprietario.getBarbearia() == null
				|| !id.equals(novoProprietario.getBarbearia().getId())) {
			throw new OperacaoBarbeariaConflitanteException(
					"O novo proprietário deve ser um profissional ativo da própria barbearia");
		}
		if (repository.existsByProprietarioIdAndAtivoTrueAndIdNot(profissionalId, id)) {
			throw new OperacaoBarbeariaConflitanteException(
					"O profissional já é proprietário de outra barbearia ativa");
		}
		barbearia.transferirPropriedadePara(novoProprietario);
		return barbearia;
	}

	@Transactional
	public void desativar(Long id) {
		Barbearia barbearia = buscarPorId(id);
		if (profissionalRepository.existsByBarbeariaIdAndAtivoTrue(id)) {
			throw new OperacaoBarbeariaConflitanteException(
					"Transfira ou desative os profissionais ativos antes de desativar a barbearia");
		}
		if (agendamentoRepository.existsByBarbeariaIdAndStatusInAndInicioAfter(
				id, StatusAgendamento.QUE_OCUPAM_HORARIO, clock.instant())) {
			throw new OperacaoBarbeariaConflitanteException(
					"A barbearia possui agendamentos futuros e não pode ser desativada");
		}
		barbearia.desativar();
	}

	// Compatibilidade interna para testes e registros legados.
	@Transactional
	public Barbearia cadastrar(String nome) {
		String normalizado = normalizarNome(nome);
		if (repository.existsByNomeIgnoreCase(normalizado)) throw new BarbeariaJaCadastradaException();
		return repository.save(new Barbearia(normalizado));
	}

	@Transactional
	public Barbearia atualizar(Long id, String nome) {
		Barbearia barbearia = buscarPorId(id);
		String normalizado = normalizarNome(nome);
		validarNomeDisponivel(normalizado, barbearia);
		barbearia.atualizarNome(normalizado);
		return barbearia;
	}

	private Barbearia cadastrar(
			Profissional proprietario,
			String nomeInformado,
			EnderecoBarbearia endereco,
			List<HorarioFuncionamentoRequest> horarios
	) {
		if (!proprietario.isAtivo() || !proprietario.getUsuario().isAtivo()) {
			throw new ProfissionalInativoException(proprietario.getId());
		}
		if (proprietario.getId() != null
				&& repository.existsByProprietarioIdAndAtivoTrue(proprietario.getId())) {
			throw new OperacaoBarbeariaConflitanteException(
					"O profissional já é proprietário de uma barbearia ativa");
		}
		String nome = normalizarNome(nomeInformado);
		if (repository.existsByNomeIgnoreCase(nome)) throw new BarbeariaJaCadastradaException();
		List<HorarioFuncionamentoRequest> horariosValidados = validarHorarios(horarios);
		validarTransferencia(proprietario, horariosValidados);

		ZoneId fusoHorario = proprietario.getFusoHorario();
		Barbearia barbearia = repository.save(new Barbearia(nome, proprietario, endereco, fusoHorario));
		horarioFuncionamentoRepository.saveAll(horariosValidados.stream()
				.map(horario -> new HorarioFuncionamentoBarbearia(
						barbearia, horario.diaSemana(), horario.horarioInicio(), horario.horarioFim()))
				.toList());
		proprietario.transferirPara(barbearia);
		return barbearia;
	}

	private void validarTransferencia(
			Profissional proprietario,
			List<HorarioFuncionamentoRequest> funcionamento
	) {
		if (proprietario.getBarbearia() != null
				&& agendamentoRepository.existsByProfissionalIdAndStatusInAndInicioAfter(
						proprietario.getId(), StatusAgendamento.QUE_OCUPAM_HORARIO, clock.instant())) {
			throw new OperacaoBarbeariaConflitanteException(
					"O profissional possui agendamentos futuros e não pode mudar de barbearia");
		}

		boolean jornadaFora = horarioAtendimentoRepository
				.findAllByProfissionalIdAndAtivoTrue(proprietario.getId())
				.stream()
				.anyMatch(jornada -> funcionamento.stream().noneMatch(abertura ->
						abertura.diaSemana() == jornada.getDiaSemana()
								&& !jornada.getHorarioInicio().isBefore(abertura.horarioInicio())
								&& !jornada.getHorarioFim().isAfter(abertura.horarioFim())));
		if (jornadaFora) {
			throw new OperacaoBarbeariaConflitanteException(
					"Os horários da barbearia precisam abranger toda a jornada atual do proprietário");
		}
	}

	private List<HorarioFuncionamentoRequest> validarHorarios(List<HorarioFuncionamentoRequest> horarios) {
		List<HorarioFuncionamentoRequest> ordenados = new ArrayList<>(horarios);
		for (HorarioFuncionamentoRequest horario : ordenados) {
			if (horario.diaSemana() == null || horario.horarioInicio() == null
					|| horario.horarioFim() == null
					|| !horario.horarioInicio().isBefore(horario.horarioFim())) {
				throw new HorarioFuncionamentoInvalidoException(
						"Todo horário deve possuir início anterior ao fim");
			}
		}
		ordenados.sort(Comparator.comparing(HorarioFuncionamentoRequest::diaSemana)
				.thenComparing(HorarioFuncionamentoRequest::horarioInicio));
		for (int i = 1; i < ordenados.size(); i++) {
			var anterior = ordenados.get(i - 1);
			var atual = ordenados.get(i);
			if (anterior.diaSemana() == atual.diaSemana()
					&& atual.horarioInicio().isBefore(anterior.horarioFim())) {
				throw new HorarioFuncionamentoSobrepostoException();
			}
		}
		return List.copyOf(ordenados);
	}

	private void validarNomeDisponivel(String nome, Barbearia atual) {
		if (!atual.getNome().equalsIgnoreCase(nome) && repository.existsByNomeIgnoreCase(nome)) {
			throw new BarbeariaJaCadastradaException();
		}
	}

	private String normalizarNome(String nome) {
		return nome.trim();
	}
}
