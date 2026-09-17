package com.agendapro.shared.imagem;

import static com.agendapro.shared.web.ApiPaths.API_V1;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(API_V1 + "/imagens")
public class ImagemController {
	private final ArmazenamentoImagemService armazenamento;

	public ImagemController(ArmazenamentoImagemService armazenamento) {
		this.armazenamento = armazenamento;
	}

	@GetMapping("/{nome:.+}")
	public ResponseEntity<?> carregar(@PathVariable String nome) {
		ImagemArmazenada imagem = armazenamento.carregar(nome);
		return ResponseEntity.ok()
				.cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic().immutable())
				.contentType(imagem.tipo())
				.body(imagem.recurso());
	}
}
