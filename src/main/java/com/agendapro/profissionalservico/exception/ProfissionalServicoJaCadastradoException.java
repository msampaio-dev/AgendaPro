package com.agendapro.profissionalservico.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProfissionalServicoJaCadastradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ProfissionalServicoJaCadastradoException(
			Long profissionalId,
			Long servicoId
	) {
		super(
				"Serviço com id " + servicoId
				+ " já está associado ao profissional com id " + profissionalId
		);
	}
}
