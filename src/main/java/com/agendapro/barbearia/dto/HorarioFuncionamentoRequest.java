package com.agendapro.barbearia.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record HorarioFuncionamentoRequest(
		@NotNull DayOfWeek diaSemana,
		@NotNull LocalTime horarioInicio,
		@NotNull LocalTime horarioFim
) {
}
