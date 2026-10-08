package com.agendapro.sugestao.dto;

import java.time.LocalDate;
import java.util.List;

import com.agendapro.disponibilidade.dto.HorarioDisponivelResponse;
import com.agendapro.sugestao.ia.PedidoInterpretado.Periodo;

/**
 * Sugestao para o cliente revisar. Nada aqui esta reservado: confirmar significa
 * chamar o POST /agendamentos de sempre, que confere tudo de novo.
 *
 * dataPedida e o que o cliente escreveu; data e onde ha vaga. Quando diferem, a
 * tela avisa que o dia pedido estava sem horario.
 */
public record SugestaoAgendamentoResponse(
		Long barbeariaId,
		String barbeariaNome,
		Long profissionalId,
		String profissionalNome,
		Long servicoId,
		String servicoNome,
		Long servicoAdicionalId,
		String servicoAdicionalNome,
		LocalDate dataPedida,
		LocalDate data,
		Periodo periodo,
		List<HorarioDisponivelResponse> horarios,
		String observacao,
		int sugestoesRestantesHoje
) {
}
