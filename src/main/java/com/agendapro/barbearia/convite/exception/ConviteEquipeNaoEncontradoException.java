package com.agendapro.barbearia.convite.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ConviteEquipeNaoEncontradoException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public ConviteEquipeNaoEncontradoException(Long id) {
		super("Convite de equipe não encontrado: " + id);
	}
}
