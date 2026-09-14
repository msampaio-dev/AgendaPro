package com.agendapro.agendamento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FiltroAgendamentoInvalidoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public FiltroAgendamentoInvalidoException(String motivo) {
		super("Filtro de agendamentos inválido: " + motivo);
	}
}
