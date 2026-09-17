package com.agendapro.usuario.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.usuario.entity.PerfilUsuario;
import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.exception.EmailJaCadastradoException;
import com.agendapro.usuario.exception.FiltroUsuarioInvalidoException;
import com.agendapro.usuario.exception.UsuarioNaoEncontradoException;
import com.agendapro.usuario.repository.UsuarioRepository;

@Service
public class UsuarioService {
	private static final Set<String> CAMPOS_ORDENACAO_PERMITIDOS = Set.of(
			"id", "nome", "email", "ativo"
	);

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

	@Transactional(readOnly = true)
	public Page<Usuario> listarParaAdministracao(
			String termo,
			Boolean ativo,
			PerfilUsuario perfil,
			Pageable pageable
	) {
		Specification<Usuario> filtro = (root, query, builder) -> builder.conjunction();

		if (termo != null && !termo.isBlank()) {
			String busca = "%" + termo.trim().toLowerCase(Locale.ROOT) + "%";
			filtro = filtro.and((root, query, builder) -> builder.or(
					builder.like(builder.lower(root.get("nome")), busca),
					builder.like(builder.lower(root.get("email")), busca)
			));
		}
		if (ativo != null) {
			filtro = filtro.and((root, query, builder) ->
					builder.equal(root.get("ativo"), ativo));
		}
		if (perfil != null) {
			filtro = filtro.and((root, query, builder) -> {
				query.distinct(true);
				return builder.equal(root.join("perfis"), perfil);
			});
		}

		return usuarioRepository.findAll(filtro, normalizarPaginacao(pageable));
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

	@Transactional
	public Usuario reativar(Long id) {
		Usuario usuario = buscarPorId(id);
		usuario.ativar();
		return usuario;
	}

	private Pageable normalizarPaginacao(Pageable pageable) {
		Sort ordenacao = pageable.getSort().isUnsorted()
				? Sort.by(Sort.Direction.ASC, "nome")
				: pageable.getSort();

		for (Sort.Order ordem : ordenacao) {
			if (!CAMPOS_ORDENACAO_PERMITIDOS.contains(ordem.getProperty())) {
				throw new FiltroUsuarioInvalidoException(
						"ordenação permitida apenas por id, nome, email ou ativo"
				);
			}
		}

		return PageRequest.of(
				pageable.getPageNumber(),
				Math.min(pageable.getPageSize(), 100),
				ordenacao
		);
	}
}
