package com.agendapro.usuario.dto;

import java.util.List;

import com.agendapro.usuario.entity.PerfilUsuario;
import com.agendapro.usuario.entity.Usuario;

public record UsuarioResponse(Long id, String nome, String email, boolean ativo, List<PerfilUsuario> perfis) {

	public static UsuarioResponse from(Usuario usuario) {
		return new UsuarioResponse(
				usuario.getId(),
				usuario.getNome(),
				usuario.getEmail(),
				usuario.isAtivo(),
				usuario.getPerfis().stream().sorted().toList());
	}
}
