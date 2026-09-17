package com.agendapro.barbearia.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class HorarioFuncionamentoInvalidoException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public HorarioFuncionamentoInvalidoException(String mensagem) {
		super(mensagem);
	}
}
