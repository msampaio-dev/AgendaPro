package com.agendapro.sugestao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class LimiteSugestoesAtingidoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public LimiteSugestoesAtingidoException() {
		super("O limite diário de sugestões foi atingido. Escolha o horário manualmente.");
	}
}
