package com.agendapro.barbearia.convite.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriacaoConviteEquipeRequest(
		@NotBlank(message = "O e-mail é obrigatório")
		@Email(message = "Informe um e-mail válido")
		@Size(max = 254)
		String email
) {
}
