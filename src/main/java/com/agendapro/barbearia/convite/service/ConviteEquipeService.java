package com.agendapro.barbearia.convite.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.barbearia.convite.entity.ConviteEquipe;
import com.agendapro.barbearia.convite.entity.StatusConviteEquipe;
import com.agendapro.barbearia.convite.exception.ConviteEquipeExpiradoException;
import com.agendapro.barbearia.convite.exception.ConviteEquipeNaoEncontradoException;
import com.agendapro.barbearia.convite.exception.OperacaoConviteEquipeConflitanteException;
import com.agendapro.barbearia.convite.repository.ConviteEquipeRepository;
import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.barbearia.repository.BarbeariaRepository;
import com.agendapro.barbearia.service.BarbeariaService;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.repository.ProfissionalRepository;
import com.agendapro.usuario.entity.PerfilUsuario;
import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.exception.UsuarioInativoException;
import com.agendapro.usuario.exception.UsuarioNaoEncontradoException;
import com.agendapro.usuario.repository.UsuarioRepository;

@Service
public class ConviteEquipeService {
	private static final Duration VALIDADE = Duration.ofDays(7);

	private final ConviteEquipeRepository repository;
	private final BarbeariaService barbeariaService;
	private final BarbeariaRepository barbeariaRepository;
	private final UsuarioRepository usuarioRepository;
	private final ProfissionalRepository profissionalRepository;
	private final Clock clock;

	public ConviteEquipeService(
			ConviteEquipeRepository repository,
			BarbeariaService barbeariaService,
			BarbeariaRepository barbeariaRepository,
			UsuarioRepository usuarioRepository,
			ProfissionalRepository profissionalRepository,
			Clock clock
	) {
		this.repository = repository;
		this.barbeariaService = barbeariaService;
		this.barbeariaRepository = barbeariaRepository;
		this.usuarioRepository = usuarioRepository;
		this.profissionalRepository = profissionalRepository;
		this.clock = clock;
	}

	@Transactional
	public ConviteEquipe criar(Long barbeariaId, String emailInformado, Long criadorUsuarioId) {
		Barbearia barbearia = barbeariaService.buscarAtivaPorId(barbeariaId);
		Usuario criador = buscarUsuario(criadorUsuarioId);
		validarCriadorPodeGerenciar(barbeariaId, criador);
		String email = normalizarEmail(emailInformado);
		validarNaoPertenceAEquipe(email);

		repository.findByBarbeariaIdAndEmailIgnoreCaseAndStatus(
				barbeariaId, email, StatusConviteEquipe.PENDENTE)
				.ifPresent(existente -> {
					Instant agora = clock.instant();
					if (existente.expirou(agora)) {
						existente.expirar(agora);
						repository.flush();
					} else {
						throw new OperacaoConviteEquipeConflitanteException(
								"Já existe um convite pendente para este e-mail");
					}
				});

		Instant agora = clock.instant();
		return repository.save(new ConviteEquipe(
				barbearia, email, criador, agora, agora.plus(VALIDADE)));
	}

	@Transactional
	public List<ConviteEquipe> listarDaBarbearia(Long barbeariaId) {
		barbeariaService.buscarPorId(barbeariaId);
		List<ConviteEquipe> convites = repository.findAllByBarbeariaIdOrderByCriadoEmDesc(barbeariaId);
		atualizarExpirados(convites);
		return convites;
	}

	@Transactional
	public List<ConviteEquipe> listarDoUsuario(Long usuarioId) {
		Usuario usuario = buscarUsuario(usuarioId);
		List<ConviteEquipe> convites = repository.findAllByEmailIgnoreCaseOrderByCriadoEmDesc(
				usuario.getEmail());
		atualizarExpirados(convites);
		return convites;
	}

	@Transactional
	public Usuario aceitar(Long conviteId, Long usuarioId) {
		ConviteEquipe convite = buscarParaAtualizacao(conviteId);
		Usuario usuario = buscarUsuario(usuarioId);
		validarDestinatario(convite, usuario);
		validarPendenteEValido(convite);
		if (!usuario.isAtivo()) throw new UsuarioInativoException(usuarioId);

		Profissional profissional = profissionalRepository.findByUsuarioId(usuarioId).orElse(null);
		if (profissional == null) {
			profissional = profissionalRepository.save(new Profissional(usuario, convite.getBarbearia()));
		} else if (profissional.isAtivo()) {
			if (!convite.getBarbearia().getId().equals(profissional.getBarbearia().getId())) {
				throw new OperacaoConviteEquipeConflitanteException(
						"Você já pertence a outra barbearia; a transferência deve ser feita pelo administrador");
			}
		} else {
			profissional.ativarNaBarbearia(convite.getBarbearia());
		}

		usuario.adicionarPerfil(PerfilUsuario.PROFISSIONAL);
		convite.aceitar(clock.instant());
		return usuario;
	}

	@Transactional
	public void recusar(Long conviteId, Long usuarioId) {
		ConviteEquipe convite = buscarParaAtualizacao(conviteId);
		Usuario usuario = buscarUsuario(usuarioId);
		validarDestinatario(convite, usuario);
		validarPendenteEValido(convite);
		convite.recusar(clock.instant());
	}

	@Transactional
	public void cancelar(Long barbeariaId, Long conviteId) {
		ConviteEquipe convite = buscarParaAtualizacao(conviteId);
		if (!barbeariaId.equals(convite.getBarbearia().getId())) {
			throw new ConviteEquipeNaoEncontradoException(conviteId);
		}
		validarPendenteEValido(convite);
		convite.cancelar(clock.instant());
	}

	private void validarCriadorPodeGerenciar(Long barbeariaId, Usuario criador) {
		boolean administrador = criador.getPerfis().contains(PerfilUsuario.ADMIN);
		boolean proprietario = barbeariaRepository.existsByIdAndProprietarioUsuarioIdAndAtivoTrue(
				barbeariaId, criador.getId());
		if (!administrador && !proprietario) {
			throw new AccessDeniedException("Usuário não pode gerenciar convites desta barbearia");
		}
	}

	private void validarNaoPertenceAEquipe(String email) {
		usuarioRepository.findByEmailIgnoreCase(email)
				.flatMap(usuario -> profissionalRepository.findByUsuarioId(usuario.getId()))
				.filter(Profissional::isAtivo)
				.ifPresent(profissional -> {
					throw new OperacaoConviteEquipeConflitanteException(
							"O usuário já possui um perfil profissional ativo");
				});
	}

	private void atualizarExpirados(List<ConviteEquipe> convites) {
		Instant agora = clock.instant();
		convites.stream().filter(convite -> convite.expirou(agora))
				.forEach(convite -> convite.expirar(agora));
	}

	private void validarPendenteEValido(ConviteEquipe convite) {
		Instant agora = clock.instant();
		if (convite.expirou(agora)) {
			convite.expirar(agora);
			throw new ConviteEquipeExpiradoException();
		}
		if (!convite.estaPendente()) {
			throw new OperacaoConviteEquipeConflitanteException(
					"O convite já foi respondido ou cancelado");
		}
	}

	private void validarDestinatario(ConviteEquipe convite, Usuario usuario) {
		if (!convite.getEmail().equalsIgnoreCase(usuario.getEmail())) {
			throw new AccessDeniedException("O convite pertence a outro usuário");
		}
	}

	private ConviteEquipe buscarParaAtualizacao(Long id) {
		return repository.buscarPorIdParaAtualizacao(id)
				.orElseThrow(() -> new ConviteEquipeNaoEncontradoException(id));
	}

	private Usuario buscarUsuario(Long id) {
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new UsuarioNaoEncontradoException(id));
	}

	private String normalizarEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
