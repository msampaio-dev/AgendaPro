package com.agendapro.disponibilidade.dto;

import java.time.LocalDate;
import java.util.List;

public record DisponibilidadeResponse(
		Long profissionalId,
		Long servicoId,
		LocalDate data,
		List<HorarioDisponivelResponse> horarios
) {
}
