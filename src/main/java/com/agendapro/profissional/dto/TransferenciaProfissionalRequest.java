package com.agendapro.profissional.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferenciaProfissionalRequest(
		@NotNull @Positive Long barbeariaId
) {
}
