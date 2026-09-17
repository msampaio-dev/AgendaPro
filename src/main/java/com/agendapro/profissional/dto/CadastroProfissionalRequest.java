package com.agendapro.profissional.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CadastroProfissionalRequest(
		@NotNull(message = "O id do usuário é obrigatório")
		@Positive(message = "O id do usuário deve ser positivo")
		Long usuarioId,

		@NotNull(message = "O id da barbearia é obrigatório")
		@Positive(message = "O id da barbearia deve ser positivo")
		Long barbeariaId
) {
}
