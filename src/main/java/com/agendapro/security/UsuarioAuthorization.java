package com.agendapro.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class UsuarioAuthorization {

	public boolean proprioUsuarioOuAdmin(Long usuarioId, Authentication authentication) {
		return possuiPerfil(authentication, "ROLE_ADMIN")
				|| usuarioId.equals(usuarioIdAutenticado(authentication));
	}

	private Long usuarioIdAutenticado(Authentication authentication) {
		try {
			return Long.valueOf(authentication.getName());
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private boolean possuiPerfil(Authentication authentication, String perfil) {
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals(perfil));
	}
}
