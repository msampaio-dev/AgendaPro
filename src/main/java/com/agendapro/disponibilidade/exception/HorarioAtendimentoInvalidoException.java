package com.agendapro.disponibilidade.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class HorarioAtendimentoInvalidoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public HorarioAtendimentoInvalidoException() {
		super("O horário de início deve ser anterior ao horário de fim");
	}
}
