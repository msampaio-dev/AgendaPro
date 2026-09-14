package com.agendapro.agendamento.dto;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.agendapro.agendamento.entity.Agendamento;
import com.agendapro.agendamento.entity.StatusAgendamento;

public record AgendamentoResponse(
		Long id,
		Long clienteId,
		Long profissionalId,
		Long servicoId,
		OffsetDateTime inicio,
		OffsetDateTime fim,
		StatusAgendamento status
) {

	public static AgendamentoResponse from(Agendamento agendamento) {
		ZoneId fusoHorario = agendamento.getProfissional().getFusoHorario();

		return new AgendamentoResponse(
				agendamento.getId(),
				agendamento.getCliente().getId(),
				agendamento.getProfissional().getId(),
				agendamento.getServico().getId(),
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
