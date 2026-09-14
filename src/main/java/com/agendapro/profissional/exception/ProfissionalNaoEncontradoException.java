package com.agendapro.profissional.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProfissionalNaoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ProfissionalNaoEncontradoException(Long id) {
		super("Profissional não encontrado com id " + id);
	}
}
