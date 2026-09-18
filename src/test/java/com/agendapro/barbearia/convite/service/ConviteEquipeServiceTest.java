package com.agendapro.barbearia.convite.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.agendapro.barbearia.convite.entity.ConviteEquipe;
import com.agendapro.barbearia.convite.entity.StatusConviteEquipe;
import com.agendapro.barbearia.convite.exception.OperacaoConviteEquipeConflitanteException;
import com.agendapro.barbearia.convite.repository.ConviteEquipeRepository;
import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.barbearia.repository.BarbeariaRepository;
import com.agendapro.barbearia.service.BarbeariaService;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.repository.ProfissionalRepository;
import com.agendapro.usuario.entity.PerfilUsuario;
import com.agendapro.usuario.entity.Usuario;
import com.agendapro.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ConviteEquipeServiceTest {
	private static final Instant AGORA = Instant.parse("2030-01-01T12:00:00Z");

	@Mock private ConviteEquipeRepository repository;
	@Mock private BarbeariaService barbeariaService;
	@Mock private BarbeariaRepository barbeariaRepository;
	@Mock private UsuarioRepository usuarioRepository;
	@Mock private ProfissionalRepository profissionalRepository;
	@Mock private Clock clock;
	@InjectMocks private ConviteEquipeService service;

	@BeforeEach
	void configurarRelogio() {
		org.mockito.Mockito.lenient().when(clock.instant()).thenReturn(AGORA);
	}

	@Test
	void deveCriarConviteNormalizandoEmail() {
		Barbearia barbearia = org.mockito.Mockito.mock(Barbearia.class);
		Usuario criador = new Usuario("Dono", "dono@teste.com", "hash");
		criador.adicionarPerfil(PerfilUsuario.ADMIN);
		when(barbeariaService.buscarAtivaPorId(3L)).thenReturn(barbearia);
		when(usuarioRepository.findById(9L)).thenReturn(Optional.of(criador));
		when(repository.findByBarbeariaIdAndEmailIgnoreCaseAndStatus(
				3L, "convidado@teste.com", StatusConviteEquipe.PENDENTE))
				.thenReturn(Optional.empty());
		when(repository.save(any(ConviteEquipe.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		ConviteEquipe convite = service.criar(3L, " Convidado@Teste.com ", 9L);

		assertEquals("convidado@teste.com", convite.getEmail());
		assertEquals(AGORA.plusSeconds(7 * 24 * 60 * 60), convite.getExpiraEm());
	}

	@Test
	void naoDeveCriarConviteSeCriadorNaoGerenciaBarbearia() {
		Barbearia barbearia = org.mockito.Mockito.mock(Barbearia.class);
		Usuario criador = new Usuario("Estranho", "estranho@teste.com", "hash");
		when(barbeariaService.buscarAtivaPorId(3L)).thenReturn(barbearia);
		when(usuarioRepository.findById(9L)).thenReturn(Optional.of(criador));
		when(barbeariaRepository.existsByIdAndProprietarioUsuarioIdAndAtivoTrue(3L, criador.getId()))
				.thenReturn(false);

		assertThrows(AccessDeniedException.class,
				() -> service.criar(3L, "convidado@teste.com", 9L));
		verify(repository, never()).save(any());
	}

	@Test
	void naoDeveCriarConviteDuplicadoPendente() {
		Barbearia barbearia = org.mockito.Mockito.mock(Barbearia.class);
		Usuario criador = new Usuario("Dono", "dono@teste.com", "hash");
		criador.adicionarPerfil(PerfilUsuario.ADMIN);
		ConviteEquipe existente = new ConviteEquipe(
				barbearia, "pessoa@teste.com", criador, AGORA, AGORA.plusSeconds(3600));
		when(barbeariaService.buscarAtivaPorId(3L)).thenReturn(barbearia);
		when(usuarioRepository.findById(9L)).thenReturn(Optional.of(criador));
		when(repository.findByBarbeariaIdAndEmailIgnoreCaseAndStatus(
				3L, "pessoa@teste.com", StatusConviteEquipe.PENDENTE))
				.thenReturn(Optional.of(existente));

		assertThrows(OperacaoConviteEquipeConflitanteException.class,
				() -> service.criar(3L, "pessoa@teste.com", 9L));
		verify(repository, never()).save(any());
	}

	@Test
	void deveCriarPerfilProfissionalAoAceitar() {
		Barbearia barbearia = org.mockito.Mockito.mock(Barbearia.class);
		Usuario criador = new Usuario("Dono", "dono@teste.com", "hash");
		Usuario convidado = new Usuario("Convidado", "convidado@teste.com", "hash");
		ConviteEquipe convite = new ConviteEquipe(
				barbearia, convidado.getEmail(), criador, AGORA, AGORA.plusSeconds(3600));
		when(repository.buscarPorIdParaAtualizacao(4L)).thenReturn(Optional.of(convite));
		when(usuarioRepository.findById(5L)).thenReturn(Optional.of(convidado));
		when(profissionalRepository.findByUsuarioId(5L)).thenReturn(Optional.empty());

		Usuario resultado = service.aceitar(4L, 5L);

		assertTrue(resultado.getPerfis().contains(PerfilUsuario.PROFISSIONAL));
		assertEquals(StatusConviteEquipe.ACEITO, convite.getStatus());
		verify(profissionalRepository).save(any(Profissional.class));
	}

	@Test
	void naoDeveAceitarConviteDeOutroEmail() {
		Barbearia barbearia = org.mockito.Mockito.mock(Barbearia.class);
		Usuario criador = new Usuario("Dono", "dono@teste.com", "hash");
		Usuario usuario = new Usuario("Outro", "outro@teste.com", "hash");
		ConviteEquipe convite = new ConviteEquipe(
				barbearia, "destino@teste.com", criador, AGORA, AGORA.plusSeconds(3600));
		when(repository.buscarPorIdParaAtualizacao(4L)).thenReturn(Optional.of(convite));
		when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));

		assertThrows(AccessDeniedException.class, () -> service.aceitar(4L, 5L));
		verify(profissionalRepository, never()).save(any());
	}
}
