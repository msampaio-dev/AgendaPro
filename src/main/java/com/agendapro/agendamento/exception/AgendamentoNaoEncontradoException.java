package com.agendapro.agendamento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AgendamentoNaoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AgendamentoNaoEncontradoException(Long id) {
		super("Agendamento não encontrado com id " + id);
	}
}
