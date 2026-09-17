package com.agendapro.barbearia.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class BarbeariaInativaException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public BarbeariaInativaException(Long id) {
		super("A barbearia " + id + " está inativa");
	}
}
