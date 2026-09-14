package com.agendapro.usuario.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EmailJaCadastradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EmailJaCadastradoException() {
		super("E-mail já cadastrado");
	}
}