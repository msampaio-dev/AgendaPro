package com.agendapro.shared;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
				.andExpect(jsonPath("$.paths['/agendamentos'].get.parameters[?(@.name == 'page')]").exists())
				.andExpect(jsonPath("$.paths['/agendamentos'].get.parameters[?(@.name == 'size')]").exists())
				.andExpect(jsonPath("$.paths['/agendamentos'].get.parameters[?(@.name == 'sort')]").exists())
				.andExpect(jsonPath("$.paths['/agendamentos'].get.parameters[?(@.name == 'pageable')]").doesNotExist());
	}

	@Test
	void deveResponderErroJsonQuandoAutenticacaoEstiverAusente() throws Exception {
		mockMvc.perform(get("/servicos"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.mensagem").exists())
				.andExpect(jsonPath("$.caminho").value("/servicos"));
	}

	@Test
	void deveResponderCamposInvalidosNoCadastro() throws Exception {
		mockMvc.perform(post("/usuarios")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.mensagem").value("Dados inválidos"))
				.andExpect(jsonPath("$.campos").isArray());
	}
}
