package com.agendapro.usuario.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UsuarioInativoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UsuarioInativoException(Long id) {
		super("Usuário inativo com id " + id);
	}
}