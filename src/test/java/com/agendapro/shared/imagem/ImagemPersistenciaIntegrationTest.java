package com.agendapro.shared.imagem;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import com.agendapro.shared.PostgresIntegrationTest;

/**
 * A foto agora vive no banco, e nao no disco efemero do ambiente publicado.
 * Este teste vai ate o PostgreSQL de verdade porque e la que o mapeamento de
 * byte[] para BYTEA pode falhar sem que nenhum teste com mock perceba.
 */
@SpringBootTest
class ImagemPersistenciaIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private ArmazenamentoImagemService armazenamento;

	@Test
	void deveDevolverOsMesmosBytesGravados() throws Exception {
		byte[] png = criarPng();

		String url = armazenamento.armazenar(
				new MockMultipartFile("arquivo", "capa.png", "image/png", png),
				"barbearia-1"
		);
		ImagemArmazenada carregada = armazenamento.carregar(nomeDe(url));

		assertArrayEquals(png, carregada.recurso().getContentAsByteArray());
		assertEquals(MediaType.IMAGE_PNG, carregada.tipo());
	}

	@Test
	void deveRemoverAImagemDoBanco() throws Exception {
		String url = armazenamento.armazenar(
				new MockMultipartFile("arquivo", "capa.png", "image/png", criarPng()),
				"barbearia-2"
		);

		armazenamento.remover(url);

		assertThrows(
				ImagemNaoEncontradaException.class,
				() -> armazenamento.carregar(nomeDe(url))
		);
	}

	private String nomeDe(String url) {
		return url.substring(url.lastIndexOf('/') + 1);
	}

	private byte[] criarPng() throws Exception {
		BufferedImage imagem = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream saida = new ByteArrayOutputStream();
		ImageIO.write(imagem, "png", saida);
		return saida.toByteArray();
	}
}
