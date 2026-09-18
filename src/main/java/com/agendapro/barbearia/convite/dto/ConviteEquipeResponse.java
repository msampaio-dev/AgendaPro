package com.agendapro.barbearia.convite.dto;

import java.time.Instant;

import com.agendapro.barbearia.convite.entity.ConviteEquipe;
import com.agendapro.barbearia.convite.entity.StatusConviteEquipe;

public record ConviteEquipeResponse(
		Long id,
		Long barbeariaId,
		String barbeariaNome,
		String email,
		StatusConviteEquipe status,
		String criadoPorNome,
		Instant criadoEm,
		Instant expiraEm,
		Instant respondidoEm
) {
	public static ConviteEquipeResponse from(ConviteEquipe convite) {
		return new ConviteEquipeResponse(
				convite.getId(),
				convite.getBarbearia().getId(),
				convite.getBarbearia().getNome(),
				convite.getEmail(),
				convite.getStatus(),
				convite.getCriadoPor().getNome(),
				convite.getCriadoEm(),
				convite.getExpiraEm(),
				convite.getRespondidoEm()
		);
	}
}
