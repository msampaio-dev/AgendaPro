package com.agendapro.profissional.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProfissionalJaCadastradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ProfissionalJaCadastradoException(Long usuarioId) {
		super("Usuário já possui perfil profissional: " + usuarioId);
	}
}