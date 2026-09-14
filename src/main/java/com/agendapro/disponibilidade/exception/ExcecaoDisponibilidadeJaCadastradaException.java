package com.agendapro.disponibilidade.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ExcecaoDisponibilidadeJaCadastradaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ExcecaoDisponibilidadeJaCadastradaException() {
		super("Já existe uma exceção de disponibilidade ativa com os mesmos dados");
	}
}
