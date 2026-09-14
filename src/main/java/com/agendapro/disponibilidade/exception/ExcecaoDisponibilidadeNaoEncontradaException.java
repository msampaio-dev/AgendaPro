package com.agendapro.disponibilidade.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ExcecaoDisponibilidadeNaoEncontradaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ExcecaoDisponibilidadeNaoEncontradaException(Long id) {
		super("Exceção de disponibilidade não encontrada com id " + id);
	}
}
