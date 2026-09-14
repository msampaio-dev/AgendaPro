package com.agendapro.disponibilidade.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.agendapro.disponibilidade.dto.CadastroHorarioAtendimentoRequest;
import com.agendapro.disponibilidade.dto.HorarioAtendimentoResponse;
import com.agendapro.disponibilidade.entity.HorarioAtendimento;
import com.agendapro.disponibilidade.service.HorarioAtendimentoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/horarios-atendimento")
public class HorarioAtendimentoController {

	private final HorarioAtendimentoService horarioAtendimentoService;

	public HorarioAtendimentoController(
			HorarioAtendimentoService horarioAtendimentoService
	) {
		this.horarioAtendimentoService = horarioAtendimentoService;
	}

	@PostMapping
	@PreAuthorize("@profissionalAuthorization.podeGerenciar(#request.profissionalId(), authentication)")
	@ResponseStatus(HttpStatus.CREATED)
	public HorarioAtendimentoResponse cadastrar(
			@Valid @RequestBody CadastroHorarioAtendimentoRequest request
	) {
		HorarioAtendimento horario = horarioAtendimentoService.cadastrar(
				request.profissionalId(),
				request.diaSemana(),
				request.horarioInicio(),
				request.horarioFim()
		);

		return HorarioAtendimentoResponse.from(horario);
	}

	@GetMapping
	public List<HorarioAtendimentoResponse> listarAtivosPorProfissional(
			@RequestParam Long profissionalId
	) {
		return horarioAtendimentoService
				.listarAtivosPorProfissional(profissionalId)
				.stream()
				.map(HorarioAtendimentoResponse::from)
				.toList();
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("@profissionalAuthorization.podeGerenciarHorario(#id, authentication)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desativar(@PathVariable Long id) {
		horarioAtendimentoService.desativar(id);
	}
}
