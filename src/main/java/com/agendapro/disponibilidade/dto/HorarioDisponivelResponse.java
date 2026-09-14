package com.agendapro.disponibilidade.dto;

import java.time.LocalTime;

public record HorarioDisponivelResponse(
		LocalTime inicio,
		LocalTime fim
) {
}
