package com.agendapro.disponibilidade.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class HorarioAtendimentoNaoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public HorarioAtendimentoNaoEncontradoException(Long id) {
		super("Horário de atendimento não encontrado com id " + id);
	}
}
