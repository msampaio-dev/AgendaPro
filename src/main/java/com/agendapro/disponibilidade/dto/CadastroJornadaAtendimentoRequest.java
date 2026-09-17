package com.agendapro.disponibilidade.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CadastroJornadaAtendimentoRequest(
		@NotNull
		@Positive
		Long profissionalId,

		@NotNull
		DayOfWeek diaSemana,

		@NotNull
		LocalTime horarioInicio,

		@NotNull
		LocalTime inicioIntervalo,

		@NotNull
		LocalTime fimIntervalo,

		@NotNull
		LocalTime horarioFim
) {
}
