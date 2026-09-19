package com.agendapro.shared.imagem;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Guarda e devolve as imagens enviadas.
 *
 * O armazenamento vive no banco: o disco do ambiente publicado e efemero e
 * perdia as fotos a cada reimplantacao. Toda a aplicacao fala com esta classe,
 * e so ela sabe onde os bytes estao — trocar por um object storage no futuro
 * fica contido aqui.
 */
@Service
public class ArmazenamentoImagemService {
	private static final long TAMANHO_MAXIMO = 5L * 1024 * 1024;
	private static final long PIXELS_MAXIMOS = 25_000_000L;
	private static final Map<String, String> EXTENSOES = Map.of(
			MediaType.IMAGE_JPEG_VALUE, ".jpg",
			MediaType.IMAGE_PNG_VALUE, ".png"
	);

	private final ImagemRepository repository;
	private final Clock clock;

	public ArmazenamentoImagemService(ImagemRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Transactional
	public String armazenar(MultipartFile arquivo, String prefixo) {
		validar(arquivo);
		String extensao = EXTENSOES.get(arquivo.getContentType());
		String nome = prefixo + "-" + UUID.randomUUID() + extensao;

		try {
			repository.save(new Imagem(
					nome,
					arquivo.getContentType(),
					arquivo.getBytes(),
					clock.instant()
			));
			return "/api/v1/imagens/" + nome;
		} catch (IOException exception) {
			throw new ArmazenamentoImagemException(exception);
		}
	}

	@Transactional(readOnly = true)
	public ImagemArmazenada carregar(String nome) {
		// A busca e por chave primaria: um nome forjado simplesmente nao existe,
		// e nao ha caminho de arquivo para escapar como havia no disco.
		Imagem imagem = repository.findById(nome)
				.orElseThrow(ImagemNaoEncontradaException::new);

		return new ImagemArmazenada(
				new ByteArrayResource(imagem.getConteudo()),
				MediaType.parseMediaType(imagem.getTipoConteudo())
		);
	}

	@Transactional
	public void remover(String fotoUrl) {
		if (fotoUrl == null || fotoUrl.isBlank()) return;
		repository.deleteById(fotoUrl.substring(fotoUrl.lastIndexOf('/') + 1));
	}

	private void validar(MultipartFile arquivo) {
		if (arquivo == null || arquivo.isEmpty()) {
			throw new ImagemInvalidaException("Selecione uma imagem");
		}
		if (arquivo.getSize() > TAMANHO_MAXIMO) {
			throw new ImagemInvalidaException("A imagem deve possuir no máximo 5 MB");
		}
		if (!EXTENSOES.containsKey(arquivo.getContentType())) {
			throw new ImagemInvalidaException("Envie uma imagem JPEG ou PNG");
		}

		try (InputStream entrada = arquivo.getInputStream()) {
			BufferedImage imagem = ImageIO.read(entrada);
			if (imagem == null || (long) imagem.getWidth() * imagem.getHeight() > PIXELS_MAXIMOS) {
				throw new ImagemInvalidaException("O arquivo não é uma imagem válida ou possui resolução excessiva");
			}
		} catch (IOException exception) {
			throw new ImagemInvalidaException("Não foi possível ler a imagem");
		}
	}
}
