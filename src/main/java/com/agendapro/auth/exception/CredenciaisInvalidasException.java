package com.agendapro.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class CredenciaisInvalidasException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CredenciaisInvalidasException() {
		super("E-mail ou senha inválidos");
	}
}
