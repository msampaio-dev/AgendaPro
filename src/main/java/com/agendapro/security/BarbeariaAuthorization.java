package com.agendapro.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.agendapro.barbearia.repository.BarbeariaRepository;
import com.agendapro.barbearia.repository.HorarioFuncionamentoBarbeariaRepository;

@Component
public class BarbeariaAuthorization {
	private final BarbeariaRepository barbeariaRepository;
	private final HorarioFuncionamentoBarbeariaRepository horarioRepository;

	public BarbeariaAuthorization(
			BarbeariaRepository barbeariaRepository,
			HorarioFuncionamentoBarbeariaRepository horarioRepository
	) {
		this.barbeariaRepository = barbeariaRepository;
		this.horarioRepository = horarioRepository;
	}

	public boolean podeGerenciar(Long barbeariaId, Authentication authentication) {
		if (administrador(authentication)) return true;
		Long usuarioId = usuarioId(authentication);
		return profissional(authentication) && usuarioId != null
				&& barbeariaRepository.existsByIdAndProprietarioUsuarioIdAndAtivoTrue(
						barbeariaId, usuarioId);
	}

	public boolean podeGerenciarHorario(Long horarioId, Authentication authentication) {
		if (administrador(authentication)) return true;
		Long usuarioId = usuarioId(authentication);
		return profissional(authentication) && usuarioId != null
				&& horarioRepository.existsByIdAndBarbeariaProprietarioUsuarioIdAndAtivoTrue(
						horarioId, usuarioId);
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
