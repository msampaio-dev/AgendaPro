package com.agendapro.shared.imagem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ArmazenamentoImagemServiceTest {
	@TempDir
	Path diretorio;

	@Test
	void deveArmazenarECarregarPngValido() throws Exception {
		ArmazenamentoImagemService service = new ArmazenamentoImagemService(diretorio.toString());
		MockMultipartFile arquivo = new MockMultipartFile(
				"arquivo", "perfil.png", "image/png", criarPng()
		);

		String url = service.armazenar(arquivo, "profissional-1");
		String nome = url.substring(url.lastIndexOf('/') + 1);
		ImagemArmazenada armazenada = service.carregar(nome);

		assertTrue(url.startsWith("/api/v1/imagens/profissional-1-"));
		assertTrue(Files.exists(diretorio.resolve(nome)));
		assertEquals("image/png", armazenada.tipo().toString());
	}

	@Test
	void deveRejeitarArquivoQueNaoSejaImagem() {
		ArmazenamentoImagemService service = new ArmazenamentoImagemService(diretorio.toString());
		MockMultipartFile arquivo = new MockMultipartFile(
				"arquivo", "falso.png", "image/png", "não é imagem".getBytes()
		);

		assertThrows(ImagemInvalidaException.class, () -> service.armazenar(arquivo, "teste"));
	}

	@Test
	void deveRejeitarImagemMaiorQueCincoMegabytes() {
		ArmazenamentoImagemService service = new ArmazenamentoImagemService(diretorio.toString());
		MockMultipartFile arquivo = new MockMultipartFile(
				"arquivo", "grande.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]
		);

		assertThrows(ImagemInvalidaException.class, () -> service.armazenar(arquivo, "teste"));
	}

	@Test
	void naoDevePermitirAcessoForaDoDiretorio() {
		ArmazenamentoImagemService service = new ArmazenamentoImagemService(diretorio.toString());
		assertThrows(ImagemInvalidaException.class, () -> service.carregar("../segredo.png"));
	}

	private byte[] criarPng() throws Exception {
		BufferedImage imagem = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream saida = new ByteArrayOutputStream();
		ImageIO.write(imagem, "png", saida);
		return saida.toByteArray();
	}
}
