package com.agendapro.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.agendapro.agendamento.repository.AgendamentoRepository;
import com.agendapro.profissional.repository.ProfissionalRepository;

@Component
public class AgendamentoAuthorization {

	private final AgendamentoRepository agendamentoRepository;
	private final ProfissionalRepository profissionalRepository;

	public AgendamentoAuthorization(
			AgendamentoRepository agendamentoRepository,
			ProfissionalRepository profissionalRepository
	) {
		this.agendamentoRepository = agendamentoRepository;
		this.profissionalRepository = profissionalRepository;
	}

	public boolean podeAgendar(Long clienteId, Authentication authentication) {
		return administrador(authentication) || clienteId.equals(usuarioId(authentication));
	}

	public boolean podeAcessar(Long agendamentoId, Authentication authentication) {
		if (administrador(authentication)) return true;
		Long usuarioId = usuarioId(authentication);
		return usuarioId != null && (
				agendamentoRepository.existsByIdAndClienteId(agendamentoId, usuarioId)
				|| agendamentoRepository
						.existsByIdAndProfissionalUsuarioIdAndProfissionalAtivoTrue(agendamentoId, usuarioId));
	}

	public boolean podeGerenciar(Long agendamentoId, Authentication authentication) {
		if (administrador(authentication)) return true;
		Long usuarioId = usuarioId(authentication);
		return profissional(authentication) && usuarioId != null
				&& agendamentoRepository
						.existsByIdAndProfissionalUsuarioIdAndProfissionalAtivoTrue(agendamentoId, usuarioId);
	}

	public boolean podeListar(Long profissionalId, Authentication authentication) {
		if (administrador(authentication)) return true;
		Long usuarioId = usuarioId(authentication);
		return profissional(authentication) && usuarioId != null
				&& profissionalRepository.existsByIdAndUsuarioIdAndAtivoTrue(profissionalId, usuarioId);
	}

	public boolean podeListarCliente(Long clienteId, Authentication authentication) {
		return administrador(authentication) || clienteId.equals(usuarioId(authentication));
	}

	private Long usuarioId(Authentication authentication) {
		try {
			return Long.valueOf(authentication.getName());
		} catch (NumberFormatException exception) {
			return null;
		}
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
}
