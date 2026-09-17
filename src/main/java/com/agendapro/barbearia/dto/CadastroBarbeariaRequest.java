package com.agendapro.barbearia.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastroBarbeariaRequest(
		@NotBlank(message = "O nome é obrigatório")
		@Size(max = 120, message = "O nome deve possuir no máximo 120 caracteres")
		String nome,

		@NotNull @Valid
		EnderecoBarbeariaRequest endereco,

		@NotEmpty(message = "Informe ao menos um horário de funcionamento")
		List<@Valid HorarioFuncionamentoRequest> horarios
) {
}
