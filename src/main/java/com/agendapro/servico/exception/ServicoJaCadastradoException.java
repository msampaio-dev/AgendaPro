package com.agendapro.servico.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ServicoJaCadastradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ServicoJaCadastradoException(String nome) {
		super("Serviço já cadastrado com o nome " + nome);
	}
}
