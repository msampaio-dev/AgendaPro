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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agendapro.profissional.dto.CadastroProfissionalRequest;
import com.agendapro.profissional.dto.ProfissionalResponse;
import com.agendapro.profissional.dto.TransferenciaProfissionalRequest;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.service.ProfissionalService;
import com.agendapro.profissional.service.TransferenciaProfissionalService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(API_V1 + "/profissionais")
public class ProfissionalController {

	private final ProfissionalService profissionalService;
	private final TransferenciaProfissionalService transferenciaService;

	public ProfissionalController(
			ProfissionalService profissionalService,
			TransferenciaProfissionalService transferenciaService
	) {
		this.profissionalService = profissionalService;
		this.transferenciaService = transferenciaService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	public ProfissionalResponse cadastrar(
			@Valid @RequestBody CadastroProfissionalRequest request
	) {
		Profissional profissional = profissionalService.cadastrar(request.usuarioId(), request.barbeariaId());

		return ProfissionalResponse.from(profissional);
	}

	@GetMapping("/{id}")
	public ProfissionalResponse buscarPorId(@PathVariable Long id) {
		Profissional profissional = profissionalService.buscarPorId(id);

		return ProfissionalResponse.from(profissional);
	}

	@GetMapping
	public List<ProfissionalResponse> listar(@RequestParam(required = false) Long barbeariaId) {
		var profissionais = barbeariaId == null
				? profissionalService.listar()
				: profissionalService.listarPorBarbearia(barbeariaId);
		return profissionais
				.stream()
				.map(ProfissionalResponse::from)
				.toList();
	}

	@PatchMapping("/{id}/barbearia")
	@PreAuthorize("hasRole('ADMIN')")
	public ProfissionalResponse transferir(
			@PathVariable Long id,
			@Valid @RequestBody TransferenciaProfissionalRequest request
	) {
		return ProfissionalResponse.from(
				transferenciaService.transferir(id, request.barbeariaId()));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("@profissionalAuthorization.podeGerenciar(#id, authentication)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desativar(@PathVariable Long id) {
		profissionalService.desativar(id);
	}
}
