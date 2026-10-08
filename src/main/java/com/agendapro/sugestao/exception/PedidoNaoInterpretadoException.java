package com.agendapro.sugestao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class PedidoNaoInterpretadoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PedidoNaoInterpretadoException() {
		this("Não consegui montar uma sugestão a partir do texto. Escolha o horário manualmente.");
	}

	public PedidoNaoInterpretadoException(String mensagem) {
		super(mensagem);
	}
}
