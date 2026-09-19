package com.agendapro.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.agendapro.barbearia.repository.BarbeariaRepository;
import com.agendapro.profissional.repository.ProfissionalRepository;
import com.agendapro.servico.repository.ServicoRepository;

/**
 * Quem pode mexer no catalogo de uma barbearia: o ADMIN, o proprietario dela e
 * qualquer profissional ativo que trabalhe la — cada um cadastra os servicos
 * que oferece, sem depender de um administrador da plataforma.
 */
@Component
public class ServicoAuthorization {

	private final ServicoRepository servicoRepository;
	private final ProfissionalRepository profissionalRepository;
	private final BarbeariaRepository barbeariaRepository;

	public ServicoAuthorization(
			ServicoRepository servicoRepository,
			ProfissionalRepository profissionalRepository,
			BarbeariaRepository barbeariaRepository
	) {
		this.servicoRepository = servicoRepository;
		this.profissionalRepository = profissionalRepository;
		this.barbeariaRepository = barbeariaRepository;
	}

	public boolean podeGerenciarCatalogo(Long barbeariaId, Authentication authentication) {
		if (administrador(authentication)) {
			return true;
		}

		Long usuarioId = usuarioId(authentication);

		if (barbeariaId == null || usuarioId == null || !profissional(authentication)) {
			return false;
		}

		return profissionalRepository
					.existsByBarbeariaIdAndUsuarioIdAndAtivoTrue(barbeariaId, usuarioId)
				|| barbeariaRepository
					.existsByIdAndProprietarioUsuarioIdAndAtivoTrue(barbeariaId, usuarioId);
	}

	public boolean podeGerenciarServico(Long servicoId, Authentication authentication) {
		if (administrador(authentication)) {
			return true;
		}

		return servicoRepository.findById(servicoId)
				.map(servico -> podeGerenciarCatalogo(
						servico.getBarbearia().getId(),
						authentication
				))
				.orElse(false);
	}

	private boolean administrador(Authentication authentication) {
		return possui(authentication, "ROLE_ADMIN");
	}

	private boolean profissional(Authentication authentication) {
		return possui(authentication, "ROLE_PROFISSIONAL");
	}

	private boolean possui(Authentication authentication, String perfil) {
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals(perfil));
	}

	private Long usuarioId(Authentication authentication) {
		try {
			return Long.valueOf(authentication.getName());
		} catch (NumberFormatException exception) {
			return null;
		}
	}
}
