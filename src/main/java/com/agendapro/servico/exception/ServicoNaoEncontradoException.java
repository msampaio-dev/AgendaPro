package com.agendapro.servico.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ServicoNaoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ServicoNaoEncontradoException(Long id) {
		super("Serviço não encontrado com id " + id);
	}
}
