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
import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.barbearia.service.BarbeariaService;
import com.agendapro.barbearia.repository.BarbeariaRepository;
import com.agendapro.barbearia.exception.OperacaoBarbeariaConflitanteException;

@Service
public class ProfissionalService {

	private final ProfissionalRepository profissionalRepository;
	private final UsuarioService usuarioService;
	private final BarbeariaService barbeariaService;
	private final BarbeariaRepository barbeariaRepository;

	public ProfissionalService(ProfissionalRepository profissionalRepository, UsuarioService usuarioService, BarbeariaService barbeariaService, BarbeariaRepository barbeariaRepository) {
		this.profissionalRepository = profissionalRepository;
		this.usuarioService = usuarioService;
		this.barbeariaService = barbeariaService;
		this.barbeariaRepository = barbeariaRepository;
	}

	@Transactional
	public Profissional cadastrar(Long usuarioId, Long barbeariaId) {
		Usuario usuario = usuarioService.buscarPorId(usuarioId);

		if (!usuario.isAtivo()) {
			throw new UsuarioInativoException(usuarioId);
		}

		if (profissionalRepository.existsByUsuarioId(usuarioId)) {
			throw new ProfissionalJaCadastradoException(usuarioId);
		}

		Barbearia barbearia = barbeariaService.buscarAtivaPorId(barbeariaId);

		Profissional profissional = new Profissional(usuario, barbearia);
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

	@Transactional(readOnly = true)
	public List<Profissional> listarPorBarbearia(Long barbeariaId) {
		barbeariaService.buscarAtivaPorId(barbeariaId);
		return profissionalRepository.findAllByBarbeariaId(barbeariaId);
	}

	@Transactional
	public void desativar(Long id) {
		Profissional profissional = buscarPorId(id);
		if (barbeariaRepository.existsByProprietarioIdAndAtivoTrue(id)) {
			throw new OperacaoBarbeariaConflitanteException(
					"Transfira a propriedade da barbearia antes de desativar o profissional");
		}
		profissional.desativar();
		profissional.getUsuario().removerPerfil(PerfilUsuario.PROFISSIONAL);
	}
}
