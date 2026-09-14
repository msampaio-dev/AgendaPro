package com.agendapro.agendamento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.agendapro.agendamento.entity.StatusAgendamento;

@ResponseStatus(HttpStatus.CONFLICT)
public class TransicaoStatusAgendamentoInvalidaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TransicaoStatusAgendamentoInvalidaException(
			StatusAgendamento statusAtual,
			StatusAgendamento statusDesejado
	) {
		super("Não é possível alterar o agendamento de %s para %s"
				.formatted(statusAtual, statusDesejado));
	}
}
