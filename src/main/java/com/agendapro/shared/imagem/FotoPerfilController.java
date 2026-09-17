package com.agendapro.shared.imagem;

import static com.agendapro.shared.web.ApiPaths.API_V1;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.agendapro.barbearia.dto.BarbeariaResponse;
import com.agendapro.profissional.dto.ProfissionalResponse;

@RestController
@RequestMapping(API_V1)
public class FotoPerfilController {
	private final FotoPerfilService service;

	public FotoPerfilController(FotoPerfilService service) {
		this.service = service;
	}

	@PostMapping(value = "/barbearias/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("@barbeariaAuthorization.podeGerenciar(#id, authentication)")
	public BarbeariaResponse atualizarFotoBarbearia(
			@PathVariable Long id,
			@RequestPart("arquivo") MultipartFile arquivo
	) {
		return BarbeariaResponse.from(service.atualizarFotoBarbearia(id, arquivo));
	}

	@DeleteMapping("/barbearias/{id}/foto")
	@PreAuthorize("@barbeariaAuthorization.podeGerenciar(#id, authentication)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removerFotoBarbearia(@PathVariable Long id) {
		service.removerFotoBarbearia(id);
	}

	@PostMapping(value = "/profissionais/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("@profissionalAuthorization.podeGerenciar(#id, authentication)")
	public ProfissionalResponse atualizarFotoProfissional(
			@PathVariable Long id,
			@RequestPart("arquivo") MultipartFile arquivo
	) {
		return ProfissionalResponse.from(service.atualizarFotoProfissional(id, arquivo));
	}

	@DeleteMapping("/profissionais/{id}/foto")
	@PreAuthorize("@profissionalAuthorization.podeGerenciar(#id, authentication)")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removerFotoProfissional(@PathVariable Long id) {
		service.removerFotoProfissional(id);
	}
}
