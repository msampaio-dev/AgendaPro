package com.agendapro.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class LoginBloqueadoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public LoginBloqueadoException() {
		super("Muitas tentativas de login. Tente novamente mais tarde.");
	}
}
