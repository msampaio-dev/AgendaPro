package com.agendapro.disponibilidade.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.agendapro.disponibilidade.entity.HorarioAtendimento;

public record HorarioAtendimentoResponse(
		Long id,
		Long profissionalId,
		DayOfWeek diaSemana,
		LocalTime horarioInicio,
		LocalTime horarioFim,
		boolean ativo
) {

	public static HorarioAtendimentoResponse from(HorarioAtendimento horario) {
		return new HorarioAtendimentoResponse(
				horario.getId(),
				horario.getProfissional().getId(),
				horario.getDiaSemana(),
				horario.getHorarioInicio(),
				horario.getHorarioFim(),
				horario.isAtivo()
		);
	}
}
