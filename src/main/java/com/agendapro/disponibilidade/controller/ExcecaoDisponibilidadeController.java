package com.agendapro.disponibilidade.controller;

import java.time.LocalDate;
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

import com.agendapro.disponibilidade.dto.CadastroExcecaoDisponibilidadeRequest;
import com.agendapro.disponibilidade.dto.ExcecaoDisponibilidadeResponse;
import com.agendapro.disponibilidade.entity.ExcecaoDisponibilidade;
import com.agendapro.disponibilidade.service.ExcecaoDisponibilidadeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/excecoes-disponibilidade")
public class ExcecaoDisponibilidadeController {

	private final ExcecaoDisponibilidadeService excecaoService;

	public ExcecaoDisponibilidadeController(
			ExcecaoDisponibilidadeService excecaoService
	) {
		this.excecaoService = excecaoService;
	}

	@PostMapping
	@PreAuthorize("@profissionalAuthorization.podeGerenciar(#request.profissionalId(), authentication)")
	@ResponseStatus(HttpStatus.CREATED)
	public ExcecaoDisponibilidadeResponse cadastrar(
			@Valid @RequestBody CadastroExcecaoDisponibilidadeRequest request
	) {
		ExcecaoDisponibilidade excecao = excecaoService.cadastrar(
				request.profissionalId(),
				request.data(),
				request.tipo(),
				request.horarioInicio(),
				request.horarioFim()
		);

		return ExcecaoDisponibilidadeResponse.from(excecao);
	}

	@GetMapping
	public List<ExcecaoDisponibilidadeResponse> listarAtivas(
			@RequestParam Long profissionalId,
			@RequestParam LocalDate data
	) {
		return excecaoService
				.listarAtivasPorProfissionalEData(profissionalId, data)
				.stream()
				.map(ExcecaoDisponibilidadeResponse::from)
				.toList();
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("@profissionalAuthorization.podeGerenciarExcecao(#id, authentication)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desativar(@PathVariable Long id) {
		excecaoService.desativar(id);
	}
}
