package com.agendapro.agendamento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class HorarioLocalInvalidoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public HorarioLocalInvalidoException() {
		super("A data e o horário são inválidos ou ambíguos no fuso do profissional");
	}
}
