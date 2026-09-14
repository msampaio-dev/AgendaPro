package com.agendapro.auth.dto;

import java.time.Instant;
import java.util.List;

import com.agendapro.auth.service.TokenGerado;
import com.agendapro.usuario.entity.PerfilUsuario;
import com.agendapro.usuario.entity.Usuario;

public record LoginResponse(
		Long id,
		String nome,
		String email,
		List<PerfilUsuario> perfis,
		String token,
		String tipo,
		Instant expiraEm
) {

	public static LoginResponse from(Usuario usuario, TokenGerado token) {
		return new LoginResponse(
				usuario.getId(),
				usuario.getNome(),
				usuario.getEmail(),
				usuario.getPerfis().stream().sorted().toList(),
				token.valor(),
				"Bearer",
				token.expiraEm());
	}
}
