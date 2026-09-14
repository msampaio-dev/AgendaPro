package com.agendapro.disponibilidade.controller;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.agendapro.disponibilidade.dto.DisponibilidadeResponse;
import com.agendapro.disponibilidade.service.DisponibilidadeService;

import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/disponibilidades")
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
			@RequestParam LocalDate data
	) {
		return disponibilidadeService.consultar(
				profissionalId,
				servicoId,
				data
		);
	}
}
