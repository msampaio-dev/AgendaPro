package com.agendapro.profissional.dto;

import com.agendapro.profissional.entity.Profissional;
import com.agendapro.usuario.entity.Usuario;

public record ProfissionalResponse(
		Long id,
		Long usuarioId,
		String nome,
		String email,
		boolean ativo,
		String fusoHorario
) {

	public static ProfissionalResponse from(Profissional profissional) {
		Usuario usuario = profissional.getUsuario();

		return new ProfissionalResponse(
				profissional.getId(),
				usuario.getId(),
				usuario.getNome(),
				usuario.getEmail(),
				profissional.isAtivo(),
				profissional.getFusoHorario().getId()
		);
	}
}
