package com.agendapro.auth.dto;

import java.util.List;

import com.agendapro.profissional.entity.Profissional;
import com.agendapro.usuario.entity.PerfilUsuario;
import com.agendapro.usuario.entity.Usuario;

public record SessaoResponse(
		Long id,
		String nome,
		String email,
		List<PerfilUsuario> perfis,
		Long profissionalId,
		String fusoHorario
) {

	public static SessaoResponse from(Usuario usuario, Profissional profissional) {
		return new SessaoResponse(
				usuario.getId(),
				usuario.getNome(),
				usuario.getEmail(),
				usuario.getPerfis().stream().sorted().toList(),
				profissional == null ? null : profissional.getId(),
				profissional == null ? null : profissional.getFusoHorario().getId()
		);
	}
}
