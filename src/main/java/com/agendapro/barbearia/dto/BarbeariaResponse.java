package com.agendapro.barbearia.dto;

import com.agendapro.barbearia.entity.Barbearia;

public record BarbeariaResponse(
		Long id,
		String nome,
		boolean ativo,
		String fotoUrl,
		Long proprietarioProfissionalId,
		String proprietarioNome,
		String cep,
		String logradouro,
		String numero,
		String complemento,
		String bairro,
		String cidade,
		String estado,
		String fusoHorario
) {
	public static BarbeariaResponse from(Barbearia barbearia) {
		var proprietario = barbearia.getProprietario();
		var endereco = barbearia.getEndereco();
		return new BarbeariaResponse(
				barbearia.getId(),
				barbearia.getNome(),
				barbearia.isAtivo(),
				barbearia.getFotoUrl(),
				proprietario == null ? null : proprietario.getId(),
				proprietario == null ? null : proprietario.getUsuario().getNome(),
				endereco == null ? null : endereco.getCep(),
				endereco == null ? null : endereco.getLogradouro(),
				endereco == null ? null : endereco.getNumero(),
				endereco == null ? null : endereco.getComplemento(),
				endereco == null ? null : endereco.getBairro(),
				endereco == null ? null : endereco.getCidade(),
				endereco == null ? null : endereco.getEstado(),
				barbearia.getFusoHorario().getId()
		);
	}
}
