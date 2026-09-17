package com.agendapro.shared.imagem;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record ImagemArmazenada(Resource recurso, MediaType tipo) {
}
