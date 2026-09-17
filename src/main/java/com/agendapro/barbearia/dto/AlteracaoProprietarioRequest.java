package com.agendapro.barbearia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AlteracaoProprietarioRequest(
		@NotNull @Positive Long profissionalId
) {
}
