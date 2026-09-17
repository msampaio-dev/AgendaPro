package com.agendapro.shared.imagem;

public class ArmazenamentoImagemException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ArmazenamentoImagemException(Throwable causa) {
		super("Não foi possível armazenar a imagem", causa);
	}
}
