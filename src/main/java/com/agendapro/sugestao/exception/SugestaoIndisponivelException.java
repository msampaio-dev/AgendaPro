package com.agendapro.sugestao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class SugestaoIndisponivelException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SugestaoIndisponivelException() {
		super("A sugestão automática não está disponível agora. Escolha o horário manualmente.");
	}
}
