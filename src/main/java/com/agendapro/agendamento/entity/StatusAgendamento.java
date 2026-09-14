package com.agendapro.agendamento.entity;

import java.util.Set;

public enum StatusAgendamento {
	AGENDADO,
	CONFIRMADO,
	CANCELADO,
	CONCLUIDO;

	public static final Set<StatusAgendamento> QUE_OCUPAM_HORARIO = Set.of(
			AGENDADO,
			CONFIRMADO
	);
}
