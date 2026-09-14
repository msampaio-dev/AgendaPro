package com.agendapro.servico.dto;

import java.math.BigDecimal;

import com.agendapro.servico.entity.Servico;

public record ServicoResponse(
		Long id,
		String nome,
		String descricao,
		Integer duracaoMinutos,
		BigDecimal preco,
		boolean ativo
) {

	public static ServicoResponse from(Servico servico) {
		return new ServicoResponse(
				servico.getId(),
				servico.getNome(),
				servico.getDescricao(),
				servico.getDuracaoMinutos(),
				servico.getPreco(),
				servico.isAtivo()
		);
	}
}
