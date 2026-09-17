package com.agendapro.barbearia.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class BarbeariaJaCadastradaException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public BarbeariaJaCadastradaException() { super("Já existe uma barbearia com este nome"); }
}
