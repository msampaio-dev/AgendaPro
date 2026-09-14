package com.agendapro.disponibilidade.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.agendapro.disponibilidade.entity.TipoExcecaoDisponibilidade;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CadastroExcecaoDisponibilidadeRequest(
		@NotNull
		@Positive
		Long profissionalId,

		@NotNull
		LocalDate data,

		@NotNull
		TipoExcecaoDisponibilidade tipo,

		LocalTime horarioInicio,
		LocalTime horarioFim
) {
}
