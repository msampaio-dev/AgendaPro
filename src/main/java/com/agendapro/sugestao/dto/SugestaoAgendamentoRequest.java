package com.agendapro.sugestao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * O tamanho maximo limita custo (cada caractere vira token pago) e a superficie
 * para texto que tente manipular o modelo. Um pedido de agendamento cabe com
 * folga em 300 caracteres.
 */
public record SugestaoAgendamentoRequest(
		@NotBlank
		@Size(max = 300)
		String texto
) {
}
