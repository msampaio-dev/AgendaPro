package com.agendapro.shared.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

public record PaginaResponse<T>(
		List<T> conteudo,
		int pagina,
		int tamanho,
		long totalElementos,
		int totalPaginas,
		boolean primeira,
		boolean ultima
) {

	public static <S, T> PaginaResponse<T> from(
			Page<S> pagina,
			Function<S, T> conversor
	) {
		return new PaginaResponse<>(
				pagina.getContent().stream().map(conversor).toList(),
				pagina.getNumber(),
				pagina.getSize(),
				pagina.getTotalElements(),
				pagina.getTotalPages(),
				pagina.isFirst(),
				pagina.isLast()
		);
	}
}
