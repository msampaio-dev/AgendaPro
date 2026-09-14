package com.agendapro.shared.exception;

import java.time.Instant;
import java.util.List;

public record ErroApiResponse(
		Instant instante,
		int status,
		String erro,
		String mensagem,
		String caminho,
		List<CampoInvalidoResponse> campos
) {
}
