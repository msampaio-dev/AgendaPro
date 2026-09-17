package com.agendapro.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.agendapro.barbearia.repository.BarbeariaRepository;
import com.agendapro.barbearia.repository.HorarioFuncionamentoBarbeariaRepository;

@ExtendWith(MockitoExtension.class)
class BarbeariaAuthorizationTest {

	@Mock private BarbeariaRepository barbeariaRepository;
	@Mock private HorarioFuncionamentoBarbeariaRepository horarioRepository;
	@InjectMocks private BarbeariaAuthorization authorization;

	@Test
	void proprietarioDeveGerenciarSomenteSuaBarbearia() {
		var autenticacao = autenticacao("12", "ROLE_PROFISSIONAL");
		when(barbeariaRepository.existsByIdAndProprietarioUsuarioIdAndAtivoTrue(3L, 12L))
				.thenReturn(true);

		assertTrue(authorization.podeGerenciar(3L, autenticacao));
		assertFalse(authorization.podeGerenciar(4L, autenticacao));
	}

	@Test
	void administradorGlobalPodeGerenciarQualquerBarbearia() {
		assertTrue(authorization.podeGerenciar(99L, autenticacao("1", "ROLE_ADMIN")));
	}

	private TestingAuthenticationToken autenticacao(String usuarioId, String perfil) {
		return new TestingAuthenticationToken(
				usuarioId,
				null,
				java.util.List.of(new SimpleGrantedAuthority(perfil))
		);
	}
}
