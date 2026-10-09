package com.agendapro.sugestao.ia;

/**
 * Transforma o texto livre do cliente em campos de agendamento.
 *
 * O resto do modulo depende desta interface, e nao do Claude: os testes do
 * servico simulam a interpretacao sem rede, e trocar de modelo ou de provedor
 * nao toca na validacao nem na busca de horarios.
 */
public interface InterpretadorPedido {

	/**
	 * @throws com.agendapro.sugestao.exception.SugestaoIndisponivelException
	 *         quando a IA nao responde (sem chave, rede, erro da API)
	 * @throws com.agendapro.sugestao.exception.PedidoNaoInterpretadoException
	 *         quando responde algo que nao da para usar (recusa, resposta
	 *         cortada, JSON fora do formato)
	 */
	PedidoInterpretado interpretar(String texto, CatalogoSugestao catalogo);
}
