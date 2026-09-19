package com.agendapro.servico.controller;

import static com.agendapro.shared.web.ApiPaths.API_V1;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.agendapro.servico.dto.CadastroServicoRequest;
import com.agendapro.servico.dto.AtualizacaoServicoRequest;
import com.agendapro.servico.dto.ServicoResponse;
import com.agendapro.servico.entity.Servico;
import com.agendapro.servico.service.ServicoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(API_V1 + "/servicos")
public class ServicoController {

	private final ServicoService servicoService;

	public ServicoController(ServicoService servicoService) {
		this.servicoService = servicoService;
	}

	@PostMapping
	@PreAuthorize("@servicoAuthorization.podeGerenciarCatalogo(#request.barbeariaId(), authentication)")
	@ResponseStatus(HttpStatus.CREATED)
	public ServicoResponse cadastrar(
			@Valid @RequestBody CadastroServicoRequest request
	) {
		Servico servico = servicoService.cadastrar(
				request.barbeariaId(),
				request.nome(),
				request.descricao(),
				request.duracaoMinutos(),
				request.preco()
		);

		return ServicoResponse.from(servico);
	}

	@GetMapping("/{id}")
	public ServicoResponse buscarPorId(@PathVariable Long id) {
		return ServicoResponse.from(servicoService.buscarPorId(id));
	}

	@GetMapping
	public List<ServicoResponse> listar(
			@RequestParam(required = false) Long barbeariaId,
			@RequestParam(required = false, defaultValue = "false") boolean apenasAtivos
	) {
		List<Servico> servicos;

		if (barbeariaId == null) {
			servicos = servicoService.listar();
		} else if (apenasAtivos) {
			servicos = servicoService.listarAtivosPorBarbearia(barbeariaId);
		} else {
			servicos = servicoService.listarPorBarbearia(barbeariaId);
		}

		return servicos.stream().map(ServicoResponse::from).toList();
	}

	@PutMapping("/{id}")
	@PreAuthorize("@servicoAuthorization.podeGerenciarServico(#id, authentication)")
	public ServicoResponse atualizar(
			@PathVariable Long id,
			@Valid @RequestBody AtualizacaoServicoRequest request
	) {
		Servico servico = servicoService.atualizar(
				id,
				request.nome(),
				request.descricao(),
				request.duracaoMinutos(),
				request.preco()
		);

		return ServicoResponse.from(servico);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("@servicoAuthorization.podeGerenciarServico(#id, authentication)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desativar(@PathVariable Long id) {
		servicoService.desativar(id);
	}
}
