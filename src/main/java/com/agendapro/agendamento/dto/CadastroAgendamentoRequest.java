package com.agendapro.agendamento.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CadastroAgendamentoRequest(
		@NotNull
		@Positive
		Long clienteId,

		@NotNull
		@Positive
		Long profissionalId,

		@NotNull
		@Positive
		Long servicoId,

		@NotNull
		LocalDate data,

		@NotNull
		LocalTime horarioInicio
) {
}
