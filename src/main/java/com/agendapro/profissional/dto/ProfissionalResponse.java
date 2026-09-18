package com.agendapro.profissional.dto;

import com.agendapro.profissional.entity.Profissional;
import com.agendapro.usuario.entity.Usuario;

public record ProfissionalResponse(
		Long id,
		Long usuarioId,
		String nome,
		String email,
		Long barbeariaId,
		String barbeariaNome,
		boolean ativo,
		String fusoHorario,
		String fotoUrl
) {

	public static ProfissionalResponse from(Profissional profissional) {
		return from(profissional, true);
	}

	public static ProfissionalResponse publico(Profissional profissional) {
		return from(profissional, false);
	}

	private static ProfissionalResponse from(Profissional profissional, boolean incluirEmail) {
		Usuario usuario = profissional.getUsuario();

		return new ProfissionalResponse(
				profissional.getId(),
				usuario.getId(),
				usuario.getNome(),
				incluirEmail ? usuario.getEmail() : null,
				profissional.getBarbearia() == null ? null : profissional.getBarbearia().getId(),
				profissional.getBarbearia() == null ? null : profissional.getBarbearia().getNome(),
				profissional.isAtivo(),
				profissional.getFusoHorario().getId(),
				profissional.getFotoUrl()
		);
	}
}
