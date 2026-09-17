package com.agendapro.barbearia.controller;

import static com.agendapro.shared.web.ApiPaths.API_V1;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.agendapro.barbearia.dto.AtualizacaoBarbeariaRequest;
import com.agendapro.barbearia.dto.AlteracaoProprietarioRequest;
import com.agendapro.barbearia.dto.CadastroBarbeariaAdminRequest;
import com.agendapro.barbearia.dto.CadastroBarbeariaRequest;
import com.agendapro.barbearia.dto.BarbeariaResponse;
import com.agendapro.barbearia.service.BarbeariaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(API_V1 + "/barbearias")
public class BarbeariaController {
	private final BarbeariaService service;
	public BarbeariaController(BarbeariaService service) { this.service = service; }

	@GetMapping
	public List<BarbeariaResponse> listar() {
		return service.listarAtivas().stream().map(BarbeariaResponse::from).toList();
	}

	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	public List<BarbeariaResponse> listarParaAdministracao() {
		return service.listar().stream().map(BarbeariaResponse::from).toList();
	}

	@PostMapping
	@PreAuthorize("hasRole('PROFISSIONAL')")
	@ResponseStatus(HttpStatus.CREATED)
	public BarbeariaResponse cadastrar(
			@Valid @RequestBody CadastroBarbeariaRequest request,
			Authentication authentication
	) {
		return BarbeariaResponse.from(
				service.cadastrarPeloProfissional(Long.valueOf(authentication.getName()), request));
	}

	@PostMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	public BarbeariaResponse cadastrarPeloAdmin(
			@Valid @RequestBody CadastroBarbeariaAdminRequest request
	) {
		return BarbeariaResponse.from(service.cadastrarPeloAdmin(request));
	}

	@GetMapping("/minhas")
	@PreAuthorize("hasRole('PROFISSIONAL')")
	public List<BarbeariaResponse> listarMinhas(Authentication authentication) {
		return service.listarDoProprietario(Long.valueOf(authentication.getName()))
				.stream().map(BarbeariaResponse::from).toList();
	}

	@PutMapping("/{id}")
	@PreAuthorize("@barbeariaAuthorization.podeGerenciar(#id, authentication)")
	public BarbeariaResponse atualizar(
			@PathVariable Long id,
			@Valid @RequestBody AtualizacaoBarbeariaRequest request
	) {
		return BarbeariaResponse.from(service.atualizar(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("@barbeariaAuthorization.podeGerenciar(#id, authentication)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desativar(@PathVariable Long id) { service.desativar(id); }

	@PatchMapping("/{id}/proprietario")
	@PreAuthorize("@barbeariaAuthorization.podeGerenciar(#id, authentication)")
	public BarbeariaResponse alterarProprietario(
			@PathVariable Long id,
			@Valid @RequestBody AlteracaoProprietarioRequest request
	) {
		return BarbeariaResponse.from(service.alterarProprietario(id, request.profissionalId()));
	}
}
