package com.agendapro.barbearia.convite.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OperacaoConviteEquipeConflitanteException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public OperacaoConviteEquipeConflitanteException(String mensagem) { super(mensagem); }
}
