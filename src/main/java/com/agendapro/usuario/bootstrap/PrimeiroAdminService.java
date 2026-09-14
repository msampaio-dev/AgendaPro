package com.agendapro.usuario.bootstrap;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.usuario.dto.CadastroUsuarioRequest;
import com.agendapro.usuario.entity.PerfilUsuario;
import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.repository.UsuarioRepository;
import com.agendapro.usuario.service.UsuarioService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Service
public class PrimeiroAdminService {

	private final UsuarioRepository usuarioRepository;
	private final UsuarioService usuarioService;
	private final Validator validator;

	public PrimeiroAdminService(
			UsuarioRepository usuarioRepository,
			UsuarioService usuarioService,
			Validator validator
	) {
		this.usuarioRepository = usuarioRepository;
		this.usuarioService = usuarioService;
		this.validator = validator;
	}

	@Transactional
	public boolean criarSeNecessario(AdminBootstrapProperties properties) {
		if (usuarioRepository.existsByPerfil(PerfilUsuario.ADMIN)) {
			return false;
		}

		CadastroUsuarioRequest dados = new CadastroUsuarioRequest(
				properties.nome(),
				properties.email(),
				properties.senha()
		);
		validar(dados);

		if (usuarioRepository.existsByEmailIgnoreCase(properties.email().trim())) {
			throw new IllegalStateException(
					"Não foi possível criar o ADMIN inicial: o e-mail informado já está cadastrado"
			);
		}

		Usuario admin = usuarioService.cadastrar(dados.nome(), dados.email(), dados.senha());
		admin.adicionarPerfil(PerfilUsuario.ADMIN);
		return true;
	}

	private void validar(CadastroUsuarioRequest dados) {
		Set<ConstraintViolation<CadastroUsuarioRequest>> violacoes = validator.validate(dados);

		if (!violacoes.isEmpty()) {
			String detalhes = violacoes.stream()
					.map(violacao -> violacao.getPropertyPath() + ": " + violacao.getMessage())
					.sorted()
					.reduce((primeiro, segundo) -> primeiro + "; " + segundo)
					.orElse("dados inválidos");

			throw new IllegalStateException("Configuração do ADMIN inicial inválida: " + detalhes);
		}
	}
}
