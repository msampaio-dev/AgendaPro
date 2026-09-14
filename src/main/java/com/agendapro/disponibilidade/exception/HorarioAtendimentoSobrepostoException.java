package com.agendapro.disponibilidade.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class HorarioAtendimentoSobrepostoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public HorarioAtendimentoSobrepostoException() {
		super("O horário de atendimento se sobrepõe a outro intervalo ativo");
	}
}
