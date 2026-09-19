package com.agendapro.servico.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CadastroServicoRequest(
		@NotNull(message = "A barbearia é obrigatória")
		Long barbeariaId,

		@NotBlank(message = "O nome é obrigatório")
		@Size(max = 120, message = "O nome deve possuir no máximo 120 caracteres")
		String nome,

		@Size(max = 500, message = "A descrição deve possuir no máximo 500 caracteres")
		String descricao,

		@NotNull(message = "A duração é obrigatória")
		@Positive(message = "A duração deve ser maior que zero")
		Integer duracaoMinutos,

		@NotNull(message = "O preço é obrigatório")
		@DecimalMin(value = "0.00", message = "O preço não pode ser negativo")
		@Digits(
				integer = 8,
				fraction = 2,
				message = "O preço deve possuir até 8 dígitos inteiros e 2 casas decimais"
		)
		BigDecimal preco
) {
}
