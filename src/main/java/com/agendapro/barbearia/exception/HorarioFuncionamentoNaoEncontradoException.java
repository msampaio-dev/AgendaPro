package com.agendapro.barbearia.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class HorarioFuncionamentoNaoEncontradoException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public HorarioFuncionamentoNaoEncontradoException(Long id) {
		super("Horário de funcionamento não encontrado: " + id);
	}
}
