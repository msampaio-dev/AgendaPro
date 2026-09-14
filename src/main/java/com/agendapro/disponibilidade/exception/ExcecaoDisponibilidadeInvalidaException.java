package com.agendapro.disponibilidade.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ExcecaoDisponibilidadeInvalidaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ExcecaoDisponibilidadeInvalidaException(String motivo) {
		super("Exceção de disponibilidade inválida: " + motivo);
	}
}
