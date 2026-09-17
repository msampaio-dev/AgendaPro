package com.agendapro.barbearia.controller;

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

import com.agendapro.barbearia.dto.HorarioFuncionamentoRequest;
import com.agendapro.barbearia.dto.HorarioFuncionamentoResponse;
import com.agendapro.barbearia.service.HorarioFuncionamentoBarbeariaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(API_V1 + "/barbearias/{barbeariaId}/horarios")
public class HorarioFuncionamentoBarbeariaController {
	private final HorarioFuncionamentoBarbeariaService service;

	public HorarioFuncionamentoBarbeariaController(HorarioFuncionamentoBarbeariaService service) {
		this.service = service;
	}

	@GetMapping
	public List<HorarioFuncionamentoResponse> listar(@PathVariable Long barbeariaId) {
		return service.listar(barbeariaId).stream().map(HorarioFuncionamentoResponse::from).toList();
	}

	@PostMapping
	@PreAuthorize("@barbeariaAuthorization.podeGerenciar(#barbeariaId, authentication)")
	@ResponseStatus(HttpStatus.CREATED)
	public HorarioFuncionamentoResponse cadastrar(
			@PathVariable Long barbeariaId,
			@Valid @RequestBody HorarioFuncionamentoRequest request
	) {
		return HorarioFuncionamentoResponse.from(service.cadastrar(
				barbeariaId, request.diaSemana(), request.horarioInicio(), request.horarioFim()));
	}

	@DeleteMapping("/{horarioId}")
	@PreAuthorize("@barbeariaAuthorization.podeGerenciarHorario(#horarioId, authentication)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desativar(@PathVariable Long barbeariaId, @PathVariable Long horarioId) {
		service.desativar(horarioId);
	}
}
