package com.agendapro.profissional.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProfissionalInativoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ProfissionalInativoException(Long id) {
		super("Profissional inativo com id " + id);
	}
}
