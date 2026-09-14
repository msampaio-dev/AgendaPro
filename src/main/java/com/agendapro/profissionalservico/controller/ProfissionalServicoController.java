package com.agendapro.profissionalservico.controller;

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
import org.springframework.web.bind.annotation.RequestParam;

import com.agendapro.profissionalservico.dto.CadastroProfissionalServicoRequest;
import com.agendapro.profissionalservico.dto.ProfissionalServicoResponse;
import com.agendapro.profissionalservico.entity.ProfissionalServico;
import com.agendapro.profissionalservico.service.ProfissionalServicoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/profissionais-servicos")
public class ProfissionalServicoController {

	private final ProfissionalServicoService profissionalServicoService;

	public ProfissionalServicoController(
			ProfissionalServicoService profissionalServicoService
	) {
		this.profissionalServicoService = profissionalServicoService;
	}

	@PostMapping
	@PreAuthorize("@profissionalAuthorization.podeGerenciar(#request.profissionalId(), authentication)")
	@ResponseStatus(HttpStatus.CREATED)
	public ProfissionalServicoResponse associar(
			@Valid @RequestBody CadastroProfissionalServicoRequest request
	) {
		ProfissionalServico profissionalServico =
				profissionalServicoService.associar(
						request.profissionalId(),
						request.servicoId()
				);

		return ProfissionalServicoResponse.from(profissionalServico);
	}

	@GetMapping
	public List<ProfissionalServicoResponse> listarAtivosPorProfissional(
			@RequestParam Long profissionalId
	) {
		return profissionalServicoService
				.listarAtivosPorProfissional(profissionalId)
				.stream()
				.map(ProfissionalServicoResponse::from)
				.toList();
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("@profissionalAuthorization.podeGerenciarAssociacao(#id, authentication)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desativar(@PathVariable Long id) {
		profissionalServicoService.desativar(id);
	}
}
