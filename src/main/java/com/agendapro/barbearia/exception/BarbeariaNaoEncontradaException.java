package com.agendapro.barbearia.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BarbeariaNaoEncontradaException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public BarbeariaNaoEncontradaException(Long id) { super("Barbearia não encontrada com id " + id); }
}
