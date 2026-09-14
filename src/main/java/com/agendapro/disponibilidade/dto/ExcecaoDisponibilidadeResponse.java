package com.agendapro.disponibilidade.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.agendapro.disponibilidade.entity.ExcecaoDisponibilidade;
import com.agendapro.disponibilidade.entity.TipoExcecaoDisponibilidade;

public record ExcecaoDisponibilidadeResponse(
		Long id,
		Long profissionalId,
		LocalDate data,
		TipoExcecaoDisponibilidade tipo,
		LocalTime horarioInicio,
		LocalTime horarioFim,
		boolean diaInteiro,
		boolean ativo
) {

	public static ExcecaoDisponibilidadeResponse from(
			ExcecaoDisponibilidade excecao
	) {
		return new ExcecaoDisponibilidadeResponse(
				excecao.getId(),
				excecao.getProfissional().getId(),
				excecao.getData(),
				excecao.getTipo(),
				excecao.getHorarioInicio(),
				excecao.getHorarioFim(),
				excecao.isDiaInteiro(),
				excecao.isAtivo()
		);
	}
}
