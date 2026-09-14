package com.agendapro.profissionalservico.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProfissionalNaoRealizaServicoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ProfissionalNaoRealizaServicoException(
			Long profissionalId,
			Long servicoId
	) {
		super(
				"O profissional com id " + profissionalId
				+ " não realiza o serviço ativo com id " + servicoId
		);
	}
}
