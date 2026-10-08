package com.agendapro.sugestao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.agendapro.shared.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

/**
 * Atravessa a pilha inteira: seguranca, validacao do corpo, limite no banco,
 * catalogo montado dos dados de demonstracao e regras reais de disponibilidade.
 * So a API do Claude e falsa, servida por um HTTP local.
 */
@SpringBootTest(properties = {
		"spring.flyway.locations=classpath:db/migration,classpath:db/devdata",
		"app.ia.sugestao.limite-por-usuario=3",
		"app.ia.anthropic.api-key=chave-de-teste"
})
@AutoConfigureMockMvc
class SugestaoAgendamentoHttpIntegrationTest extends PostgresIntegrationTest {

	private static final HttpServer CLAUDE_FALSO = iniciarClaudeFalso();
	private static volatile int statusClaude = 200;
	private static volatile String respostaClaude = "";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbc;

	@DynamicPropertySource
	static void apontarParaClaudeFalso(DynamicPropertyRegistry registry) {
		registry.add("app.ia.anthropic.base-url",
				() -> "http://localhost:" + CLAUDE_FALSO.getAddress().getPort());
	}

	@AfterAll
	static void pararClaudeFalso() {
		CLAUDE_FALSO.stop(0);
	}

	@BeforeEach
	void zerarUsos() {
		jdbc.execute("TRUNCATE TABLE usos_sugestao_agendamento");
	}

	@Test
	void deveDevolverSugestaoComHorariosReaisDaAgenda() throws Exception {
		Long joao = jdbc.queryForObject("""
				SELECT p.id FROM profissionais p JOIN usuarios u ON u.id = p.usuario_id
				WHERE u.email = 'joao.gabriel@demo.agendapro.local'""", Long.class);
		Long barbearia = jdbc.queryForObject(
				"SELECT barbearia_id FROM profissionais WHERE id = ?", Long.class, joao);
		Long corte = servicoDa(barbearia, "Corte de cabelo Social");
		Long barba = servicoDa(barbearia, "Barba");
		responderClaude("""
				{"barbeariaId":%d,"profissionalId":%d,"servicoId":%d,"servicoAdicionalId":%d,\
				"data":null,"periodo":"QUALQUER","observacao":""}""".formatted(barbearia, joao, corte, barba));

		mockMvc.perform(post("/api/v1/sugestoes-agendamento")
				.header("Authorization", "Bearer " + tokenDeCliente("feliz@teste.com"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\":\"corte e barba com o joao\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.profissionalNome").value("João Gabriel"))
				.andExpect(jsonPath("$.servicoAdicionalNome").value("Barba"))
				.andExpect(jsonPath("$.horarios[0].inicio").exists())
				.andExpect(jsonPath("$.sugestoesRestantesHoje").value(2));
	}

	@Test
	void deveResponder422QuandoAIaInventaUmId() throws Exception {
		responderClaude("""
				{"barbeariaId":null,"profissionalId":null,"servicoId":987654,"servicoAdicionalId":null,\
				"data":null,"periodo":"QUALQUER","observacao":""}""");

		mockMvc.perform(post("/api/v1/sugestoes-agendamento")
				.header("Authorization", "Bearer " + tokenDeCliente("inventado@teste.com"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\":\"quero fazer as unhas\"}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.mensagem").exists());
	}

	@Test
	void deveResponder503QuandoAApiDoClaudeFalha() throws Exception {
		statusClaude = 529;
		respostaClaude = """
				{"type":"error","error":{"type":"overloaded_error","message":"Overloaded"}}""";

		mockMvc.perform(post("/api/v1/sugestoes-agendamento")
				.header("Authorization", "Bearer " + tokenDeCliente("sobrecarga@teste.com"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\":\"corte amanha\"}"))
				.andExpect(status().isServiceUnavailable());

		// A IA nao respondeu, entao o uso foi devolvido.
		assertEquals(0, jdbc.queryForObject(
				"SELECT COUNT(*) FROM usos_sugestao_agendamento", Integer.class));
	}

	@Test
	void deveBloquearDepoisDoLimiteDiario() throws Exception {
		// Respostas invalidas contam no limite: a IA respondeu e a chamada foi paga.
		responderClaude("""
				{"barbeariaId":null,"profissionalId":null,"servicoId":987654,"servicoAdicionalId":null,\
				"data":null,"periodo":"QUALQUER","observacao":""}""");
		String token = tokenDeCliente("insistente@teste.com");

		for (int tentativa = 0; tentativa < 3; tentativa++) {
			pedir(token).andExpect(status().isUnprocessableEntity());
		}
		pedir(token).andExpect(status().isTooManyRequests());
	}

	@Test
	void deveValidarOTextoAntesDeContarUso() throws Exception {
		String token = tokenDeCliente("validacao@teste.com");

		mockMvc.perform(post("/api/v1/sugestoes-agendamento")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\":\"   \"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/v1/sugestoes-agendamento")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\":\"%s\"}".formatted("a".repeat(301))))
				.andExpect(status().isBadRequest());

		assertEquals(0, jdbc.queryForObject(
				"SELECT COUNT(*) FROM usos_sugestao_agendamento", Integer.class));
	}

	@Test
	void deveExigirAutenticacao() throws Exception {
		mockMvc.perform(post("/api/v1/sugestoes-agendamento")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\":\"corte amanha\"}"))
				.andExpect(status().isUnauthorized());
	}

	private ResultActions pedir(String token) throws Exception {
		return mockMvc.perform(post("/api/v1/sugestoes-agendamento")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"texto\":\"corte amanha\"}"));
	}

	private Long servicoDa(Long barbeariaId, String nome) {
		return jdbc.queryForObject(
				"SELECT id FROM servicos WHERE barbearia_id = ? AND nome = ?", Long.class, barbeariaId, nome);
	}

	private String tokenDeCliente(String email) throws Exception {
		mockMvc.perform(post("/api/v1/usuarios")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Cliente","email":"%s","senha":"senha123"}""".formatted(email)))
				.andExpect(status().isCreated());
		String login = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","senha":"senha123"}""".formatted(email)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(login).get("token").asText();
	}

	private static void responderClaude(String json) {
		statusClaude = 200;
		respostaClaude = """
				{"id":"msg_teste","type":"message","role":"assistant","model":"claude-haiku-5-5",\
				"content":[{"type":"text","text":%s}],"stop_reason":"end_turn","stop_sequence":null,\
				"usage":{"input_tokens":900,"output_tokens":120}}"""
				.formatted("\"" + json.replace("\"", "\\\"") + "\"");
	}

	private static HttpServer iniciarClaudeFalso() {
		try {
			HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			servidor.createContext("/v1/messages", troca -> {
				troca.getRequestBody().readAllBytes();
				byte[] corpo = respostaClaude.getBytes(StandardCharsets.UTF_8);
				troca.getResponseHeaders().add("Content-Type", "application/json");
				troca.getResponseHeaders().add("retry-after-ms", "1");
				troca.sendResponseHeaders(statusClaude, corpo.length);
				try (OutputStream saida = troca.getResponseBody()) {
					saida.write(corpo);
				}
			});
			servidor.start();
			return servidor;
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
