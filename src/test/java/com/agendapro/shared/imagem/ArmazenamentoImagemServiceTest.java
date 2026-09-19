package com.agendapro.shared.imagem;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ArmazenamentoImagemServiceTest {

	private final Map<String, Imagem> guardadas = new HashMap<>();
	private final ImagemRepository repository = mock(ImagemRepository.class);
	private final Clock relogio = Clock.fixed(Instant.parse("2026-09-19T12:00:00Z"), ZoneOffset.UTC);
	private final ArmazenamentoImagemService service =
			new ArmazenamentoImagemService(repository, relogio);

	@BeforeEach
	void prepararRepositorioEmMemoria() {
		when(repository.save(any())).thenAnswer(invocacao -> {
			Imagem imagem = invocacao.getArgument(0);
			guardadas.put(imagem.getNome(), imagem);
			return imagem;
		});
		when(repository.findById(any()))
				.thenAnswer(invocacao -> Optional.ofNullable(guardadas.get(invocacao.getArgument(0))));
	}

	@Test
	void deveArmazenarECarregarPngValido() throws Exception {
		byte[] png = criarPng();
		MockMultipartFile arquivo = new MockMultipartFile("arquivo", "perfil.png", "image/png", png);

		String url = service.armazenar(arquivo, "profissional-1");
		String nome = url.substring(url.lastIndexOf('/') + 1);
		ImagemArmazenada armazenada = service.carregar(nome);

		assertTrue(url.startsWith("/api/v1/imagens/profissional-1-"));
		assertEquals("image/png", armazenada.tipo().toString());
		assertArrayEquals(png, guardadas.get(nome).getConteudo());
		assertEquals(relogio.instant(), guardadas.get(nome).getCriadoEm());
	}

	@Test
	void deveRemoverPelaUrlGuardadaNoPerfil() {
		service.remover("/api/v1/imagens/profissional-1-abc.png");
		verify(repository).deleteById("profissional-1-abc.png");
	}

	@Test
	void naoDeveTentarRemoverQuandoNaoHaFoto() {
		service.remover(null);
		service.remover("  ");
		verify(repository, never()).deleteById(any());
	}

	@Test
	void deveRejeitarArquivoQueNaoSejaImagem() {
		MockMultipartFile arquivo = new MockMultipartFile(
				"arquivo", "falso.png", "image/png", "não é imagem".getBytes()
		);

		assertThrows(ImagemInvalidaException.class, () -> service.armazenar(arquivo, "teste"));
		verify(repository, never()).save(any());
	}

	@Test
	void deveRejeitarImagemMaiorQueCincoMegabytes() {
		MockMultipartFile arquivo = new MockMultipartFile(
				"arquivo", "grande.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]
		);

		assertThrows(ImagemInvalidaException.class, () -> service.armazenar(arquivo, "teste"));
		verify(repository, never()).save(any());
	}

	@Test
	void naoDeveEncontrarImagemInexistente() {
		// Antes o nome virava caminho de arquivo e precisava de guarda contra
		// escapar do diretorio; agora e chave primaria e simplesmente nao existe.
		assertThrows(ImagemNaoEncontradaException.class, () -> service.carregar("../segredo.png"));
	}

	private byte[] criarPng() throws Exception {
		BufferedImage imagem = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream saida = new ByteArrayOutputStream();
		ImageIO.write(imagem, "png", saida);
		return saida.toByteArray();
	}
}
