package com.agendapro.sugestao.ia;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Formato exato da resposta do Claude. O SDK gera o JSON Schema a partir deste
 * record e a API so devolve JSON que obedece ao schema (saida estruturada).
 *
 * Optional vira "integer ou null" no schema: o modelo e obrigado a mandar o
 * campo, mas pode dizer que o texto nao informou. O schema garante o formato,
 * nao o significado; um id que nao existe passa por aqui e e barrado na
 * validacao do servico.
 *
 * A data vem como texto de proposito: o parse fica no nosso codigo, que decide
 * o que fazer com uma data invalida.
 */
public record PedidoInterpretado(
		@JsonPropertyDescription("Id da barbearia do catalogo, ou null se o texto nao permite saber")
		Optional<Long> barbeariaId,

		@JsonPropertyDescription("Id do profissional do catalogo, ou null se o cliente nao citou nenhum")
		Optional<Long> profissionalId,

		@JsonPropertyDescription("Id do servico principal do catalogo, ou null se nenhum servico foi identificado")
		Optional<Long> servicoId,

		@JsonPropertyDescription("Id de um segundo servico pedido junto, da mesma barbearia, ou null")
		Optional<Long> servicoAdicionalId,

		@JsonPropertyDescription("Data pedida no formato AAAA-MM-DD, ou null se o cliente nao disse o dia")
		Optional<String> data,

		@JsonPropertyDescription("Periodo do dia pedido pelo cliente")
		Periodo periodo,

		@JsonPropertyDescription("Uma frase curta em portugues, para o cliente, explicando escolhas ou duvidas. Vazia se nao houver")
		String observacao
) {

	public enum Periodo {
		MANHA,
		TARDE,
		NOITE,
		QUALQUER
	}
}
