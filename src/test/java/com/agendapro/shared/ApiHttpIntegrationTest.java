package com.agendapro.shared;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiHttpIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void deveExporDocumentacaoOpenApiSemAutenticacao() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("AgendaPro API"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/api/v1/agendamentos'].get.parameters[?(@.name == 'page')]").exists())
				.andExpect(jsonPath("$.paths['/api/v1/agendamentos'].get.parameters[?(@.name == 'size')]").exists())
				.andExpect(jsonPath("$.paths['/api/v1/agendamentos'].get.parameters[?(@.name == 'sort')]").exists())
				.andExpect(jsonPath("$.paths['/api/v1/agendamentos'].get.parameters[?(@.name == 'pageable')]").doesNotExist());
	}

	@Test
	void deveExporHealthCheckSemAutenticacao() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void deveResponderErroJsonQuandoAutenticacaoEstiverAusente() throws Exception {
		mockMvc.perform(get("/api/v1/servicos"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.mensagem").exists())
				.andExpect(jsonPath("$.caminho").value("/api/v1/servicos"));
	}

	@Test
	void deveResponderCamposInvalidosNoCadastro() throws Exception {
		mockMvc.perform(post("/api/v1/usuarios")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.mensagem").value("Dados inválidos"))
				.andExpect(jsonPath("$.campos").isArray());
	}

	@Test
	void devePermitirPreflightSomenteParaOrigemConfigurada() throws Exception {
		mockMvc.perform(options("/api/v1/usuarios")
				.header("Origin", "http://localhost:5173")
				.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

		mockMvc.perform(options("/api/v1/usuarios")
				.header("Origin", "https://origem-nao-autorizada.example")
				.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}
}
