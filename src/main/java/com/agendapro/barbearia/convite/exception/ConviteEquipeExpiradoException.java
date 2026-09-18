package com.agendapro.barbearia.convite.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class ConviteEquipeExpiradoException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public ConviteEquipeExpiradoException() { super("O convite expirou"); }
}
