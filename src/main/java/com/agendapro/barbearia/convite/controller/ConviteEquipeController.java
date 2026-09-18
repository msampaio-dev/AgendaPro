package com.agendapro.barbearia.convite.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.agendapro.auth.dto.LoginResponse;
import com.agendapro.auth.service.TokenGerado;
import com.agendapro.auth.service.TokenService;
import com.agendapro.barbearia.convite.dto.ConviteEquipeResponse;
import com.agendapro.barbearia.convite.dto.CriacaoConviteEquipeRequest;
import com.agendapro.barbearia.convite.service.ConviteEquipeService;
import com.agendapro.usuario.entity.Usuario;

import jakarta.validation.Valid;

@RestController
public class ConviteEquipeController {
	private final ConviteEquipeService service;
	private final TokenService tokenService;

	public ConviteEquipeController(ConviteEquipeService service, TokenService tokenService) {
		this.service = service;
		this.tokenService = tokenService;
	}

	@PostMapping(API_V1 + "/barbearias/{barbeariaId}/convites")
	@PreAuthorize("@barbeariaAuthorization.podeGerenciar(#barbeariaId, authentication)")
	@ResponseStatus(HttpStatus.CREATED)
	public ConviteEquipeResponse criar(
			@PathVariable Long barbeariaId,
			@Valid @RequestBody CriacaoConviteEquipeRequest request,
			Authentication authentication
	) {
		return ConviteEquipeResponse.from(service.criar(
				barbeariaId, request.email(), usuarioId(authentication)));
	}

	@GetMapping(API_V1 + "/barbearias/{barbeariaId}/convites")
	@PreAuthorize("@barbeariaAuthorization.podeGerenciar(#barbeariaId, authentication)")
	public List<ConviteEquipeResponse> listarDaBarbearia(@PathVariable Long barbeariaId) {
		return service.listarDaBarbearia(barbeariaId).stream()
				.map(ConviteEquipeResponse::from).toList();
	}

	@DeleteMapping(API_V1 + "/barbearias/{barbeariaId}/convites/{conviteId}")
	@PreAuthorize("@barbeariaAuthorization.podeGerenciar(#barbeariaId, authentication)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancelar(@PathVariable Long barbeariaId, @PathVariable Long conviteId) {
		service.cancelar(barbeariaId, conviteId);
	}

	@GetMapping(API_V1 + "/convites-equipe/meus")
	public List<ConviteEquipeResponse> listarMeus(Authentication authentication) {
		return service.listarDoUsuario(usuarioId(authentication)).stream()
				.map(ConviteEquipeResponse::from).toList();
	}

	@PostMapping(API_V1 + "/convites-equipe/{conviteId}/aceitar")
	public LoginResponse aceitar(
			@PathVariable Long conviteId,
			Authentication authentication
	) {
		Usuario usuario = service.aceitar(conviteId, usuarioId(authentication));
		TokenGerado token = tokenService.gerar(usuario);
		return LoginResponse.from(usuario, token);
	}

	@PostMapping(API_V1 + "/convites-equipe/{conviteId}/recusar")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void recusar(
			@PathVariable Long conviteId,
			Authentication authentication
	) {
		service.recusar(conviteId, usuarioId(authentication));
	}

	private Long usuarioId(Authentication authentication) {
		return Long.valueOf(authentication.getName());
	}
}
