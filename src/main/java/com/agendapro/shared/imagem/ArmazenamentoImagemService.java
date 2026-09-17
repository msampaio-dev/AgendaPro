package com.agendapro.shared.imagem;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ArmazenamentoImagemService {
	private static final long TAMANHO_MAXIMO = 5L * 1024 * 1024;
	private static final long PIXELS_MAXIMOS = 25_000_000L;
	private static final Map<String, String> EXTENSOES = Map.of(
			MediaType.IMAGE_JPEG_VALUE, ".jpg",
			MediaType.IMAGE_PNG_VALUE, ".png"
	);

	private final Path diretorio;

	public ArmazenamentoImagemService(
			@Value("${app.upload.directory:${user.dir}/uploads}") String diretorio
	) {
		this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
	}

	public String armazenar(MultipartFile arquivo, String prefixo) {
		validar(arquivo);
		String extensao = EXTENSOES.get(arquivo.getContentType());
		String nome = prefixo + "-" + UUID.randomUUID() + extensao;
		Path destino = diretorio.resolve(nome).normalize();

		try {
			Files.createDirectories(diretorio);
			try (InputStream entrada = arquivo.getInputStream()) {
				Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
			}
			return "/api/v1/imagens/" + nome;
		} catch (IOException exception) {
			throw new ArmazenamentoImagemException(exception);
		}
	}

	public ImagemArmazenada carregar(String nome) {
		if (!Path.of(nome).getFileName().toString().equals(nome)) {
			throw new ImagemInvalidaException("Nome de imagem inválido");
		}

		Path arquivo = diretorio.resolve(nome).normalize();
		if (!arquivo.startsWith(diretorio) || !Files.isRegularFile(arquivo)) {
			throw new ImagemNaoEncontradaException();
		}

		try {
			Resource recurso = new UrlResource(arquivo.toUri());
			String tipoDetectado = Files.probeContentType(arquivo);
			MediaType tipo = tipoDetectado == null
					? MediaType.APPLICATION_OCTET_STREAM
					: MediaType.parseMediaType(tipoDetectado);
			return new ImagemArmazenada(recurso, tipo);
		} catch (IOException exception) {
			throw new ArmazenamentoImagemException(exception);
		}
	}

	public void remover(String fotoUrl) {
		if (fotoUrl == null || fotoUrl.isBlank()) return;
		String nome = fotoUrl.substring(fotoUrl.lastIndexOf('/') + 1);
		Path arquivo = diretorio.resolve(nome).normalize();
		if (!arquivo.startsWith(diretorio)) return;
		try {
			Files.deleteIfExists(arquivo);
		} catch (IOException exception) {
			throw new ArmazenamentoImagemException(exception);
		}
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
