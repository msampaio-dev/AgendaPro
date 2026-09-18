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
	private final LoginRateLimiter rateLimiter;
	private final String senhaHashFicticia;

	public AuthService(
			UsuarioRepository usuarioRepository,
			ProfissionalRepository profissionalRepository,
			PasswordEncoder passwordEncoder,
			LoginRateLimiter rateLimiter
	) {
		this.usuarioRepository = usuarioRepository;
		this.profissionalRepository = profissionalRepository;
		this.passwordEncoder = passwordEncoder;
		this.rateLimiter = rateLimiter;
		this.senhaHashFicticia = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
	}

	@Transactional(readOnly = true)
	public Usuario autenticar(String email, String senha) {
		String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
		rateLimiter.verificarBloqueio(emailNormalizado);

		Usuario usuario = usuarioRepository.findByEmailIgnoreCase(emailNormalizado).orElse(null);

		// Sempre executa o matches, mesmo com e-mail inexistente, para o tempo de
		// resposta não vazar se uma conta existe (mitigação de timing attack).
		String hashParaComparar = usuario != null ? usuario.getSenhaHash() : senhaHashFicticia;
		boolean senhaCorreta = passwordEncoder.matches(senha, hashParaComparar);

		if (usuario == null || !usuario.isAtivo() || !senhaCorreta) {
			rateLimiter.registrarFalha(emailNormalizado);
			throw new CredenciaisInvalidasException();
		}

		rateLimiter.registrarSucesso(emailNormalizado);
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
