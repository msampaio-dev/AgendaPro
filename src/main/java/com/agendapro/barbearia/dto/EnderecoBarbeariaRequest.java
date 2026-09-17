package com.agendapro.barbearia.dto;

import com.agendapro.barbearia.entity.EnderecoBarbearia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoBarbeariaRequest(
		@NotBlank(message = "O CEP é obrigatório")
		@Pattern(regexp = "\\d{8}", message = "O CEP deve possuir 8 números")
		String cep,

		@NotBlank(message = "O logradouro é obrigatório")
		@Size(max = 160)
		String logradouro,

		@NotBlank(message = "O número é obrigatório")
		@Size(max = 20)
		String numero,

		@Size(max = 80)
		String complemento,

		@NotBlank(message = "O bairro é obrigatório")
		@Size(max = 100)
		String bairro,

		@NotBlank(message = "A cidade é obrigatória")
		@Size(max = 100)
		String cidade,

		@NotBlank(message = "O estado é obrigatório")
		@Pattern(regexp = "[A-Za-z]{2}", message = "O estado deve possuir duas letras")
		String estado
) {
	public EnderecoBarbearia toEntity() {
		return new EnderecoBarbearia(
				cep, logradouro, numero, complemento, bairro, cidade, estado
		);
	}
}
