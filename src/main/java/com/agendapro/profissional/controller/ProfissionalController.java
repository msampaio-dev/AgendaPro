package com.agendapro.profissional.controller;

import static com.agendapro.shared.web.ApiPaths.API_V1;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
import com.agendapro.security.BarbeariaAuthorization;
import com.agendapro.security.ProfissionalAuthorization;

import jakarta.validation.Valid;

@RestController
@RequestMapping(API_V1 + "/profissionais")
public class ProfissionalController {

	private final ProfissionalService profissionalService;
	private final TransferenciaProfissionalService transferenciaService;
	private final ProfissionalAuthorization profissionalAuthorization;
	private final BarbeariaAuthorization barbeariaAuthorization;

	public ProfissionalController(
			ProfissionalService profissionalService,
			TransferenciaProfissionalService transferenciaService,
			ProfissionalAuthorization profissionalAuthorization,
			BarbeariaAuthorization barbeariaAuthorization
	) {
		this.profissionalService = profissionalService;
		this.transferenciaService = transferenciaService;
		this.profissionalAuthorization = profissionalAuthorization;
		this.barbeariaAuthorization = barbeariaAuthorization;
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
	public ProfissionalResponse buscarPorId(@PathVariable Long id, Authentication authentication) {
		Profissional profissional = profissionalService.buscarPorId(id);

		return profissionalAuthorization.podeGerenciar(id, authentication)
				? ProfissionalResponse.from(profissional)
				: ProfissionalResponse.publico(profissional);
	}

	@GetMapping
	@PreAuthorize("#barbeariaId != null or hasRole('ADMIN')")
	public List<ProfissionalResponse> listar(
			@RequestParam(required = false) Long barbeariaId,
			Authentication authentication
	) {
		var profissionais = barbeariaId == null
				? profissionalService.listar()
				: profissionalService.listarPorBarbearia(barbeariaId);
		// Sem barbeariaId só admin chega aqui (@PreAuthorize); com barbeariaId,
		// só quem administra aquela unidade vê o e-mail da própria equipe.
		boolean podeVerDetalhes = barbeariaId == null
				|| barbeariaAuthorization.podeGerenciar(barbeariaId, authentication);
		return profissionais
				.stream()
				.map(podeVerDetalhes ? ProfissionalResponse::from : ProfissionalResponse::publico)
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
