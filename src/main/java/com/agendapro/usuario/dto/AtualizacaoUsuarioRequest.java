package com.agendapro.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizacaoUsuarioRequest(

		@NotBlank(message = "O nome é obrigatório") @Size(max = 120, message = "O nome deve possuir no máximo 120 caracteres") String nome,

		@NotBlank(message = "O e-mail é obrigatório") @Email(message = "O e-mail deve ser válido") @Size(max = 254, message = "O e-mail deve possuir no máximo 254 caracteres") String email

) {
}