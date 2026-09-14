package com.agendapro.agendamento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AtendimentoAindaNaoFinalizadoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AtendimentoAindaNaoFinalizadoException() {
		super("O atendimento só pode ser concluído depois do horário de término");
	}
}
