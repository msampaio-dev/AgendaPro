package com.agendapro.profissionalservico.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CadastroProfissionalServicoRequest(
		@NotNull
		@Positive
		Long profissionalId,

		@NotNull
		@Positive
		Long servicoId
) {
}
