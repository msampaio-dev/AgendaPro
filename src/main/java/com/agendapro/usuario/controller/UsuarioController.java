package com.agendapro.usuario.controller;

import static com.agendapro.shared.web.ApiPaths.API_V1;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.agendapro.usuario.dto.AtualizacaoUsuarioRequest;
import com.agendapro.usuario.dto.CadastroUsuarioRequest;
import com.agendapro.usuario.dto.UsuarioResponse;
import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.entity.PerfilUsuario;
import com.agendapro.usuario.service.UsuarioService;
import com.agendapro.shared.dto.PaginaResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping(API_V1 + "/usuarios")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Cadastrar usuário", security = {})
	public UsuarioResponse cadastrar(@Valid @RequestBody CadastroUsuarioRequest request) {
		Usuario usuario = usuarioService.cadastrar(request.nome(), request.email(), request.senha());

		return UsuarioResponse.from(usuario);
	}

	@GetMapping("/{id}")
	@PreAuthorize("@usuarioAuthorization.proprioUsuarioOuAdmin(#id, authentication)")
	public UsuarioResponse buscarPorId(@PathVariable Long id) {
		Usuario usuario = usuarioService.buscarPorId(id);

		return UsuarioResponse.from(usuario);
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public List<UsuarioResponse> listar() {
		return usuarioService.listar().stream().map(UsuarioResponse::from).toList();
	}

	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	public PaginaResponse<UsuarioResponse> listarParaAdministracao(
			@RequestParam(required = false) String termo,
			@RequestParam(required = false) Boolean ativo,
			@RequestParam(required = false) PerfilUsuario perfil,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable
	) {
		return PaginaResponse.from(
				usuarioService.listarParaAdministracao(termo, ativo, perfil, pageable),
				UsuarioResponse::from
		);
	}

	@PutMapping("/{id}")
	@PreAuthorize("@usuarioAuthorization.proprioUsuarioOuAdmin(#id, authentication)")
	public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody AtualizacaoUsuarioRequest request) {
		Usuario usuario = usuarioService.atualizar(id, request.nome(), request.email());

		return UsuarioResponse.from(usuario);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("@usuarioAuthorization.proprioUsuarioOuAdmin(#id, authentication)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desativar(@PathVariable Long id) {
		usuarioService.desativar(id);
	}

	@PatchMapping("/admin/{id}/reativar")
	@PreAuthorize("hasRole('ADMIN')")
	public UsuarioResponse reativar(@PathVariable Long id) {
		return UsuarioResponse.from(usuarioService.reativar(id));
	}

}
