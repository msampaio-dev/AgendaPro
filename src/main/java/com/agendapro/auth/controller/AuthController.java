package com.agendapro.auth.controller;

import static com.agendapro.shared.web.ApiPaths.API_V1;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agendapro.auth.dto.LoginRequest;
import com.agendapro.auth.dto.LoginResponse;
import com.agendapro.auth.dto.SessaoResponse;
import com.agendapro.auth.service.AuthService;
import com.agendapro.auth.service.TokenGerado;
import com.agendapro.auth.service.TokenService;
import com.agendapro.usuario.entity.Usuario;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping(API_V1 + "/auth")
public class AuthController {

	private final AuthService authService;
	private final TokenService tokenService;

	public AuthController(AuthService authService, TokenService tokenService) {
		this.authService = authService;
		this.tokenService = tokenService;
	}

	@PostMapping("/login")
	@Operation(summary = "Autenticar usuário", security = {})
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		Usuario usuario = authService.autenticar(request.email(), request.senha());
		TokenGerado token = tokenService.gerar(usuario);
		return LoginResponse.from(usuario, token);
	}

	@GetMapping("/me")
	@Operation(summary = "Consultar sessão do usuário autenticado")
	public SessaoResponse me(@AuthenticationPrincipal Jwt jwt) {
		return authService.buscarSessao(Long.valueOf(jwt.getSubject()));
	}
}
