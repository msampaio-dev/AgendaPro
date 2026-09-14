package com.agendapro.profissional.controller;

import static com.agendapro.shared.web.ApiPaths.API_V1;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.agendapro.profissional.dto.CadastroProfissionalRequest;
import com.agendapro.profissional.dto.ProfissionalResponse;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.service.ProfissionalService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(API_V1 + "/profissionais")
public class ProfissionalController {

	private final ProfissionalService profissionalService;

	public ProfissionalController(ProfissionalService profissionalService) {
		this.profissionalService = profissionalService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	public ProfissionalResponse cadastrar(
			@Valid @RequestBody CadastroProfissionalRequest request
	) {
		Profissional profissional = profissionalService.cadastrar(request.usuarioId());

		return ProfissionalResponse.from(profissional);
	}

	@GetMapping("/{id}")
	public ProfissionalResponse buscarPorId(@PathVariable Long id) {
		Profissional profissional = profissionalService.buscarPorId(id);

		return ProfissionalResponse.from(profissional);
	}

	@GetMapping
	public List<ProfissionalResponse> listar() {
		return profissionalService.listar()
				.stream()
				.map(ProfissionalResponse::from)
				.toList();
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("@profissionalAuthorization.podeGerenciar(#id, authentication)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desativar(@PathVariable Long id) {
		profissionalService.desativar(id);
	}
}
