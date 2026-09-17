package com.agendapro.agendamento.controller;

import static com.agendapro.shared.web.ApiPaths.API_V1;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.agendapro.agendamento.dto.AgendamentoResponse;
import com.agendapro.agendamento.dto.CadastroAgendamentoRequest;
import com.agendapro.agendamento.entity.Agendamento;
import com.agendapro.agendamento.entity.StatusAgendamento;
import com.agendapro.agendamento.service.AgendamentoService;
import com.agendapro.shared.dto.PaginaResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping(API_V1 + "/agendamentos")
public class AgendamentoController {

	private final AgendamentoService agendamentoService;

	public AgendamentoController(AgendamentoService agendamentoService) {
		this.agendamentoService = agendamentoService;
	}

	@PostMapping
	@PreAuthorize("@agendamentoAuthorization.podeAgendar(#request.clienteId(), authentication)")
	@ResponseStatus(HttpStatus.CREATED)
	public AgendamentoResponse agendar(
			@Valid @RequestBody CadastroAgendamentoRequest request
	) {
		Agendamento agendamento = agendamentoService.agendar(
				request.clienteId(),
				request.profissionalId(),
				request.servicoId(),
				request.servicoAdicionalId(),
				request.data(),
				request.horarioInicio()
		);

		return AgendamentoResponse.from(agendamento);
	}

	@GetMapping("/{id}")
	@PreAuthorize("@agendamentoAuthorization.podeAcessar(#id, authentication)")
	public AgendamentoResponse buscarPorId(@PathVariable Long id) {
		return AgendamentoResponse.from(agendamentoService.buscarPorId(id));
	}

	@GetMapping
	@PreAuthorize("@agendamentoAuthorization.podeListar(#profissionalId, authentication)")
	public PaginaResponse<AgendamentoResponse> listarPorProfissional(
			@RequestParam Long profissionalId,
			@RequestParam(required = false) LocalDate data,
			@RequestParam(required = false) LocalDate dataInicio,
			@RequestParam(required = false) LocalDate dataFim,
			@RequestParam(required = false) StatusAgendamento status,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable
	) {
		return PaginaResponse.from(
				agendamentoService.listarPorProfissional(
						profissionalId,
						data,
						dataInicio,
						dataFim,
						status,
						pageable
				),
				AgendamentoResponse::from
		);
	}

	@GetMapping("/cliente/{clienteId}")
	@PreAuthorize("@agendamentoAuthorization.podeListarCliente(#clienteId, authentication)")
	public PaginaResponse<AgendamentoResponse> listarPorCliente(
			@PathVariable Long clienteId,
			@RequestParam(required = false) OffsetDateTime inicioDe,
			@RequestParam(required = false) OffsetDateTime inicioAntesDe,
			@RequestParam(required = false) StatusAgendamento status,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable
	) {
		return PaginaResponse.from(
				agendamentoService.listarPorCliente(
						clienteId,
						inicioDe,
						inicioAntesDe,
						status,
						pageable
				),
				AgendamentoResponse::from
		);
	}

	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	public PaginaResponse<AgendamentoResponse> listarParaAdministracao(
			@RequestParam(required = false) Long profissionalId,
			@RequestParam(required = false) OffsetDateTime inicioDe,
			@RequestParam(required = false) OffsetDateTime inicioAntesDe,
			@RequestParam(required = false) StatusAgendamento status,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable
	) {
		return PaginaResponse.from(
				agendamentoService.listarParaAdministracao(
						profissionalId,
						inicioDe,
						inicioAntesDe,
						status,
						pageable
				),
				AgendamentoResponse::from
		);
	}

	@PatchMapping("/{id}/confirmar")
	@PreAuthorize("@agendamentoAuthorization.podeGerenciar(#id, authentication)")
	public AgendamentoResponse confirmar(@PathVariable Long id) {
		return AgendamentoResponse.from(agendamentoService.confirmar(id));
	}

	@PatchMapping("/{id}/cancelar")
	@PreAuthorize("@agendamentoAuthorization.podeAcessar(#id, authentication)")
	public AgendamentoResponse cancelar(@PathVariable Long id) {
		return AgendamentoResponse.from(agendamentoService.cancelar(id));
	}

	@PatchMapping("/{id}/concluir")
	@PreAuthorize("@agendamentoAuthorization.podeGerenciar(#id, authentication)")
	public AgendamentoResponse concluir(@PathVariable Long id) {
		return AgendamentoResponse.from(agendamentoService.concluir(id));
	}
}
