package com.agendapro.barbearia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizacaoBarbeariaRequest(
		@NotBlank(message = "O nome é obrigatório")
		@Size(max = 120)
		String nome,

		@NotNull @Valid
		EnderecoBarbeariaRequest endereco
) {
}
