package com.agendapro.profissionalservico.dto;

import com.agendapro.profissionalservico.entity.ProfissionalServico;

public record ProfissionalServicoResponse(
		Long id,
		Long profissionalId,
		Long servicoId,
		boolean ativo
) {

	public static ProfissionalServicoResponse from(
			ProfissionalServico profissionalServico
	) {
		return new ProfissionalServicoResponse(
				profissionalServico.getId(),
				profissionalServico.getProfissional().getId(),
				profissionalServico.getServico().getId(),
				profissionalServico.isAtivo()
		);
	}
}
