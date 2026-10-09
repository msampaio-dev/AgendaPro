package com.agendapro.sugestao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A IA nao entregou resposta utilizavel por falha de infraestrutura.
 *
 * Para o cliente toda indisponibilidade e igual (503). Para o limite de custo
 * nao: um pedido que a Anthropic recusou ou que nem saiu daqui nao e cobrado e
 * pode voltar para a cota; um que estourou o tempo pode ter sido processado e
 * cobrado mesmo sem resposta, e devolver esse uso deixaria o teto diario sem
 * efeito justamente quando a API esta lenta.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class SugestaoIndisponivelException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final boolean podeTerSidoCobrada;

	private SugestaoIndisponivelException(boolean podeTerSidoCobrada) {
		super("A sugestão automática não está disponível agora. Escolha o horário manualmente.");
		this.podeTerSidoCobrada = podeTerSidoCobrada;
	}

	/** Sem chave, conexao recusada ou erro devolvido pela API: nada foi cobrado. */
	public static SugestaoIndisponivelException semCobranca() {
		return new SugestaoIndisponivelException(false);
	}

	/** Timeout ou resposta ilegivel: a API pode ter processado e cobrado. */
	public static SugestaoIndisponivelException possivelmenteCobrada() {
		return new SugestaoIndisponivelException(true);
	}

	public boolean podeTerSidoCobrada() {
		return podeTerSidoCobrada;
	}
}
