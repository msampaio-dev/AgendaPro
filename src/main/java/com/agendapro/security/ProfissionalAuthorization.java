package com.agendapro.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.agendapro.disponibilidade.repository.ExcecaoDisponibilidadeRepository;
import com.agendapro.disponibilidade.repository.HorarioAtendimentoRepository;
import com.agendapro.profissional.repository.ProfissionalRepository;
import com.agendapro.profissionalservico.repository.ProfissionalServicoRepository;

@Component
public class ProfissionalAuthorization {

	private final ProfissionalRepository profissionalRepository;
	private final ProfissionalServicoRepository profissionalServicoRepository;
	private final HorarioAtendimentoRepository horarioRepository;
	private final ExcecaoDisponibilidadeRepository excecaoRepository;

	public ProfissionalAuthorization(
			ProfissionalRepository profissionalRepository,
			ProfissionalServicoRepository profissionalServicoRepository,
			HorarioAtendimentoRepository horarioRepository,
			ExcecaoDisponibilidadeRepository excecaoRepository
	) {
		this.profissionalRepository = profissionalRepository;
		this.profissionalServicoRepository = profissionalServicoRepository;
		this.horarioRepository = horarioRepository;
		this.excecaoRepository = excecaoRepository;
	}

	public boolean podeGerenciar(Long profissionalId, Authentication authentication) {
		if (administrador(authentication)) {
			return true;
		}
		Long usuarioId = usuarioId(authentication);
		return profissional(authentication) && usuarioId != null
				&& (profissionalRepository.existsByIdAndUsuarioIdAndAtivoTrue(profissionalId, usuarioId)
						|| profissionalRepository
								.existsByIdAndBarbeariaProprietarioUsuarioIdAndAtivoTrue(profissionalId, usuarioId));
	}

	public boolean podeGerenciarAssociacao(Long associacaoId, Authentication authentication) {
		if (administrador(authentication)) return true;
		Long usuarioId = usuarioId(authentication);
		return profissional(authentication) && usuarioId != null
				&& (profissionalServicoRepository
						.existsByIdAndProfissionalUsuarioIdAndProfissionalAtivoTrue(associacaoId, usuarioId)
						|| profissionalServicoRepository
								.existsByIdAndProfissionalBarbeariaProprietarioUsuarioIdAndProfissionalAtivoTrue(
										associacaoId, usuarioId));
	}

	public boolean podeGerenciarHorario(Long horarioId, Authentication authentication) {
		if (administrador(authentication)) return true;
		Long usuarioId = usuarioId(authentication);
		return profissional(authentication) && usuarioId != null
				&& (horarioRepository
						.existsByIdAndProfissionalUsuarioIdAndProfissionalAtivoTrue(horarioId, usuarioId)
						|| horarioRepository
								.existsByIdAndProfissionalBarbeariaProprietarioUsuarioIdAndProfissionalAtivoTrue(
										horarioId, usuarioId));
	}

	public boolean podeGerenciarExcecao(Long excecaoId, Authentication authentication) {
		if (administrador(authentication)) return true;
		Long usuarioId = usuarioId(authentication);
		return profissional(authentication) && usuarioId != null
				&& (excecaoRepository
						.existsByIdAndProfissionalUsuarioIdAndProfissionalAtivoTrue(excecaoId, usuarioId)
						|| excecaoRepository
								.existsByIdAndProfissionalBarbeariaProprietarioUsuarioIdAndProfissionalAtivoTrue(
										excecaoId, usuarioId));
	}

	private boolean administrador(Authentication authentication) {
		return possui(authentication, "ROLE_ADMIN");
	}

	private boolean profissional(Authentication authentication) {
		return possui(authentication, "ROLE_PROFISSIONAL");
	}

	private boolean possui(Authentication authentication, String perfil) {
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals(perfil));
	}

	private Long usuarioId(Authentication authentication) {
		try {
			return Long.valueOf(authentication.getName());
		} catch (NumberFormatException exception) {
			return null;
		}
	}
}
