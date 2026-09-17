package com.agendapro.agendamento.dto;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.agendapro.agendamento.entity.Agendamento;
import com.agendapro.agendamento.entity.StatusAgendamento;

public record AgendamentoResponse(
		Long id,
		Long clienteId,
		String clienteNome,
		Long profissionalId,
		String profissionalNome,
		Long barbeariaId,
		String barbeariaNome,
		Long servicoId,
		String servicoNome,
		Long servicoAdicionalId,
		String servicoAdicionalNome,
		OffsetDateTime inicio,
		OffsetDateTime fim,
		StatusAgendamento status
) {

	public static AgendamentoResponse from(Agendamento agendamento) {
		ZoneId fusoHorario = agendamento.getProfissional().getFusoHorario();

		return new AgendamentoResponse(
				agendamento.getId(),
				agendamento.getCliente().getId(),
				agendamento.getCliente().getNome(),
				agendamento.getProfissional().getId(),
				agendamento.getProfissional().getUsuario().getNome(),
				agendamento.getBarbearia().getId(),
				agendamento.getBarbearia().getNome(),
				agendamento.getServico().getId(),
				agendamento.getServico().getNome(),
				agendamento.getServicoAdicional() == null ? null : agendamento.getServicoAdicional().getId(),
				agendamento.getServicoAdicional() == null ? null : agendamento.getServicoAdicional().getNome(),
				agendamento.getInicio()
						.atZone(fusoHorario)
						.toOffsetDateTime(),
				agendamento.getFim()
						.atZone(fusoHorario)
						.toOffsetDateTime(),
				agendamento.getStatus()
		);
	}
}
