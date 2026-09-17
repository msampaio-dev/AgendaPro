package com.agendapro.barbearia.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.agendapro.barbearia.entity.HorarioFuncionamentoBarbearia;

public record HorarioFuncionamentoResponse(
		Long id,
		Long barbeariaId,
		DayOfWeek diaSemana,
		LocalTime horarioInicio,
		LocalTime horarioFim
) {
	public static HorarioFuncionamentoResponse from(HorarioFuncionamentoBarbearia horario) {
		return new HorarioFuncionamentoResponse(
				horario.getId(), horario.getBarbearia().getId(), horario.getDiaSemana(),
				horario.getHorarioInicio(), horario.getHorarioFim()
		);
	}
}
