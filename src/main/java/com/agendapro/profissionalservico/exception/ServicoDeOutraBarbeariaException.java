package com.agendapro.profissionalservico.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ServicoDeOutraBarbeariaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ServicoDeOutraBarbeariaException(Long profissionalId, Long servicoId) {
		super(
				"O serviço com id " + servicoId
				+ " pertence a outra barbearia e não pode ser executado pelo"
				+ " profissional com id " + profissionalId
		);
	}
}
