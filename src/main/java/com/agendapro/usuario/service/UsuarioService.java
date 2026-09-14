package com.agendapro.usuario.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.exception.EmailJaCadastradoException;
import com.agendapro.usuario.exception.UsuarioNaoEncontradoException;
import com.agendapro.usuario.repository.UsuarioRepository;

@Service
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public Usuario cadastrar(String nome, String email, String senha) {
		String nomeNormalizado = nome.trim();
		String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

		if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
			throw new EmailJaCadastradoException();
		}

		String senhaHash = passwordEncoder.encode(senha);
		Usuario usuario = new Usuario(nomeNormalizado, emailNormalizado, senhaHash);

		return usuarioRepository.save(usuario);
	}

	@Transactional(readOnly = true)
	public Usuario buscarPorId(Long id) {
		return usuarioRepository.findById(id).orElseThrow(() -> new UsuarioNaoEncontradoException(id));
	}

	@Transactional(readOnly = true)
	public List<Usuario> listar() {
		return usuarioRepository.findAll();
	}

	@Transactional
	public Usuario atualizar(Long id, String nome, String email) {
		Usuario usuario = buscarPorId(id);

		String nomeNormalizado = nome.trim();
		String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

		boolean emailFoiAlterado = !usuario.getEmail().equalsIgnoreCase(emailNormalizado);

		if (emailFoiAlterado && usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
			throw new EmailJaCadastradoException();
		}

		usuario.atualizarDados(nomeNormalizado, emailNormalizado);

		return usuario;
	}

	@Transactional
	public void desativar(Long id) {
		Usuario usuario = buscarPorId(id);
		usuario.desativar();
	}
}
