package com.agendapro.disponibilidade.controller;

import static com.agendapro.shared.web.ApiPaths.API_V1;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.agendapro.disponibilidade.dto.DisponibilidadeResponse;
import com.agendapro.disponibilidade.service.DisponibilidadeService;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@RequestMapping(API_V1 + "/disponibilidades")
public class DisponibilidadeController {

	private final DisponibilidadeService disponibilidadeService;

	public DisponibilidadeController(
			DisponibilidadeService disponibilidadeService
	) {
		this.disponibilidadeService = disponibilidadeService;
	}

	@GetMapping
	public DisponibilidadeResponse consultar(
			@RequestParam @Positive Long profissionalId,
			@RequestParam @Positive Long servicoId,
			@RequestParam(required = false) @Positive Long servicoAdicionalId,
			@RequestParam LocalDate data
	) {
		return disponibilidadeService.consultar(
				profissionalId,
				servicoId,
				servicoAdicionalId,
				data
		);
	}

	@GetMapping("/proximas")
	public List<DisponibilidadeResponse> consultarProximasDatas(
			@RequestParam @Positive Long profissionalId,
			@RequestParam @Positive Long servicoId,
			@RequestParam(required = false) @Positive Long servicoAdicionalId,
			@RequestParam LocalDate dataInicial,
			@RequestParam(defaultValue = "5") @Min(1) @Max(10) int quantidade,
			@RequestParam(defaultValue = "30") @Min(1) @Max(90) int horizonteDias
	) {
		return disponibilidadeService.consultarProximasDatas(
				profissionalId,
				servicoId,
				servicoAdicionalId,
				dataInicial,
				quantidade,
				horizonteDias
		);
	}
}
