package com.agendapro.profissional.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.exception.ProfissionalJaCadastradoException;
import com.agendapro.profissional.exception.ProfissionalNaoEncontradoException;
import com.agendapro.profissional.repository.ProfissionalRepository;
import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.entity.PerfilUsuario;
import com.agendapro.usuario.exception.UsuarioInativoException;
import com.agendapro.usuario.service.UsuarioService;

@Service
public class ProfissionalService {

	private final ProfissionalRepository profissionalRepository;
	private final UsuarioService usuarioService;

	public ProfissionalService(ProfissionalRepository profissionalRepository, UsuarioService usuarioService) {
		this.profissionalRepository = profissionalRepository;
		this.usuarioService = usuarioService;
	}

	@Transactional
	public Profissional cadastrar(Long usuarioId) {
		Usuario usuario = usuarioService.buscarPorId(usuarioId);

		if (!usuario.isAtivo()) {
			throw new UsuarioInativoException(usuarioId);
		}

		if (profissionalRepository.existsByUsuarioId(usuarioId)) {
			throw new ProfissionalJaCadastradoException(usuarioId);
		}

		Profissional profissional = new Profissional(usuario);
		usuario.adicionarPerfil(PerfilUsuario.PROFISSIONAL);

		return profissionalRepository.save(profissional);
	}

	@Transactional(readOnly = true)
	public Profissional buscarPorId(Long id) {
		return profissionalRepository.findById(id)
				.orElseThrow(() -> new ProfissionalNaoEncontradoException(id));
	}

	@Transactional(readOnly = true)
	public List<Profissional> listar() {
		return profissionalRepository.findAll();
	}

	@Transactional
	public void desativar(Long id) {
		Profissional profissional = buscarPorId(id);
		profissional.desativar();
		profissional.getUsuario().removerPerfil(PerfilUsuario.PROFISSIONAL);
	}
}
