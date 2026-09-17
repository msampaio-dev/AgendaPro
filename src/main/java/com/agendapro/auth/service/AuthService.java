package com.agendapro.auth.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.auth.dto.SessaoResponse;
import com.agendapro.auth.exception.CredenciaisInvalidasException;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.repository.ProfissionalRepository;
import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.exception.UsuarioNaoEncontradoException;
import com.agendapro.usuario.repository.UsuarioRepository;

@Service
public class AuthService {

	private final UsuarioRepository usuarioRepository;
	private final ProfissionalRepository profissionalRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(
			UsuarioRepository usuarioRepository,
			ProfissionalRepository profissionalRepository,
			PasswordEncoder passwordEncoder
	) {
		this.usuarioRepository = usuarioRepository;
		this.profissionalRepository = profissionalRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public Usuario autenticar(String email, String senha) {
		String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

		Usuario usuario = usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
				.orElseThrow(CredenciaisInvalidasException::new);

		if (!usuario.isAtivo() || !passwordEncoder.matches(senha, usuario.getSenhaHash())) {
			throw new CredenciaisInvalidasException();
		}

		return usuario;
	}

	@Transactional(readOnly = true)
	public SessaoResponse buscarSessao(Long usuarioId) {
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new UsuarioNaoEncontradoException(usuarioId));
		Profissional profissional = profissionalRepository.findByUsuarioId(usuarioId).orElse(null);

		return SessaoResponse.from(usuario, profissional);
	}
}
