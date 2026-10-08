package com.agendapro.sugestao.controller;

import static com.agendapro.shared.web.ApiPaths.API_V1;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agendapro.sugestao.dto.SugestaoAgendamentoRequest;
import com.agendapro.sugestao.dto.SugestaoAgendamentoResponse;
import com.agendapro.sugestao.service.SugestaoAgendamentoService;

import jakarta.validation.Valid;

/**
 * POST porque cada chamada tem efeito: conta no limite diario e gasta uma
 * chamada paga a IA. Um GET poderia ser repetido por cache, prefetch ou crawler.
 */
@RestController
@RequestMapping(API_V1 + "/sugestoes-agendamento")
public class SugestaoAgendamentoController {

	private final SugestaoAgendamentoService sugestaoService;

	public SugestaoAgendamentoController(SugestaoAgendamentoService sugestaoService) {
		this.sugestaoService = sugestaoService;
	}

	@PostMapping
	@PreAuthorize("hasRole('CLIENTE')")
	public SugestaoAgendamentoResponse sugerir(
			@Valid @RequestBody SugestaoAgendamentoRequest request,
			Authentication authentication
	) {
		return sugestaoService.sugerir(Long.valueOf(authentication.getName()), request.texto());
	}
}
