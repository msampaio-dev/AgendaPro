package com.agendapro.agendamento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ServicoAdicionalInvalidoException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public ServicoAdicionalInvalidoException() {
		super("O serviço adicional deve ser diferente do serviço principal");
	}
}
