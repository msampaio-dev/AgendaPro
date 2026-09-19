package com.agendapro.shared.startup;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.agendapro.barbearia.service.BarbeariaService;

/**
 * Paga, logo depois do arranque, o custo que o lazy initialization empurra para
 * a primeira requisicao.
 *
 * Com spring.main.lazy-initialization ligado o boot termina a tempo do health
 * check da plataforma, mas quem chega primeiro espera o Hibernate subir e a
 * cadeia de filtros do Spring Security nascer. No ambiente publicado isso custou
 * 823ms na primeira chamada contra 252ms nas seguintes.
 *
 * O aquecimento roda fora da thread principal: a aplicacao ja esta respondendo
 * enquanto ele acontece, e qualquer falha aqui e apenas registrada. Aquecer e
 * uma otimizacao, nunca um motivo para a aplicacao nao subir.
 */
@Component
@ConditionalOnProperty(name = "app.aquecimento.enabled", havingValue = "true")
public class AquecimentoAplicacao {

	private static final Logger log = LoggerFactory.getLogger(AquecimentoAplicacao.class);

	/** Caminhos baratos que atravessam a cadeia de filtros e o DispatcherServlet. */
	private static final List<String> CAMINHOS = List.of("/actuator/health", "/api/v1/barbearias");

	private final BarbeariaService barbeariaService;
	private final AtomicInteger porta = new AtomicInteger();
	private final AtomicBoolean concluido = new AtomicBoolean();

	public AquecimentoAplicacao(BarbeariaService barbeariaService) {
		this.barbeariaService = barbeariaService;
	}

	@EventListener
	void registrarPorta(WebServerInitializedEvent evento) {
		porta.set(evento.getWebServer().getPort());
	}

	@EventListener
	void aoFicarPronta(ApplicationReadyEvent evento) {
		Thread.ofVirtual().name("aquecimento").start(this::aquecer);
	}

	/** Indica que o aquecimento terminou, com ou sem falhas pelo caminho. */
	public boolean concluido() {
		return concluido.get();
	}

	private void aquecer() {
		long inicio = System.nanoTime();
		aquecerPersistencia();
		aquecerHttp();
		concluido.set(true);
		log.info("Aquecimento concluido em {}ms", Duration.ofNanos(System.nanoTime() - inicio).toMillis());
	}

	/** Sobe o Hibernate, o pool de conexoes e o caminho de leitura do dominio. */
	private void aquecerPersistencia() {
		try {
			barbeariaService.listarAtivas();
		} catch (RuntimeException excecao) {
			log.warn("Aquecimento da persistencia falhou: {}", excecao.getMessage());
		}
	}

	private void aquecerHttp() {
		int portaAtual = porta.get();

		if (portaAtual <= 0) {
			log.warn("Aquecimento HTTP ignorado: porta do servidor desconhecida");
			return;
		}

		HttpClient cliente = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(5))
				.build();

		for (String caminho : CAMINHOS) {
			chamar(cliente, URI.create("http://127.0.0.1:" + portaAtual + caminho));
		}
	}

	private void chamar(HttpClient cliente, URI destino) {
		try {
			// A resposta nao importa: 401 aquece a cadeia de seguranca tao bem
			// quanto 200. O que se quer e ter passado por ela uma vez.
			cliente.send(
					HttpRequest.newBuilder(destino).GET().timeout(Duration.ofSeconds(10)).build(),
					HttpResponse.BodyHandlers.discarding()
			);
		} catch (InterruptedException excecao) {
			Thread.currentThread().interrupt();
		} catch (Exception excecao) {
			log.warn("Aquecimento de {} falhou: {}", destino.getPath(), excecao.getMessage());
		}
	}
}
