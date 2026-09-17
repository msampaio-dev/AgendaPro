package com.agendapro.barbearia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.barbearia.dto.CadastroBarbeariaRequest;
import com.agendapro.barbearia.dto.EnderecoBarbeariaRequest;
import com.agendapro.barbearia.dto.HorarioFuncionamentoRequest;
import com.agendapro.barbearia.exception.BarbeariaJaCadastradaException;
import com.agendapro.barbearia.exception.HorarioFuncionamentoSobrepostoException;
import com.agendapro.barbearia.exception.OperacaoBarbeariaConflitanteException;
import com.agendapro.barbearia.repository.BarbeariaRepository;
import com.agendapro.barbearia.repository.HorarioFuncionamentoBarbeariaRepository;
import com.agendapro.agendamento.entity.StatusAgendamento;
import com.agendapro.agendamento.repository.AgendamentoRepository;
import com.agendapro.disponibilidade.repository.HorarioAtendimentoRepository;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.profissional.repository.ProfissionalRepository;
import com.agendapro.usuario.entity.Usuario;

@ExtendWith(MockitoExtension.class)
class BarbeariaServiceTest {

	@Mock
	private BarbeariaRepository repository;
	@Mock private ProfissionalRepository profissionalRepository;
	@Mock private HorarioFuncionamentoBarbeariaRepository horarioFuncionamentoRepository;
	@Mock private HorarioAtendimentoRepository horarioAtendimentoRepository;
	@Mock private AgendamentoRepository agendamentoRepository;
	@Mock private Clock clock;

	@InjectMocks
	private BarbeariaService service;

	@Test
	void deveNormalizarNomeAoCadastrar() {
		when(repository.existsByNomeIgnoreCase("Barbershopping Ipanema"))
				.thenReturn(false);
		when(repository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

		Barbearia cadastrada = service.cadastrar("  Barbershopping Ipanema  ");

		assertEquals("Barbershopping Ipanema", cadastrada.getNome());
		verify(repository).save(any(Barbearia.class));
	}

	@Test
	void naoDeveCadastrarNomeDuplicadoIgnorandoMaiusculas() {
		when(repository.existsByNomeIgnoreCase("barbershopping ipanema"))
				.thenReturn(true);

		assertThrows(
				BarbeariaJaCadastradaException.class,
				() -> service.cadastrar("barbershopping ipanema")
		);
		verify(repository, never()).save(any());
	}

	@Test
	void profissionalDeveVirarProprietarioEMembroDaNovaBarbearia() {
		Usuario usuario = new Usuario("João", "joao@teste.com", "hash");
		Profissional profissional = new Profissional(usuario, new Barbearia("Antiga"));
		when(profissionalRepository.findByUsuarioIdAndAtivoTrue(7L)).thenReturn(java.util.Optional.of(profissional));
		when(repository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
		when(repository.existsByNomeIgnoreCase("Nova Barbearia")).thenReturn(false);
		when(horarioAtendimentoRepository.findAllByProfissionalIdAndAtivoTrue(null)).thenReturn(List.of());

		Barbearia criada = service.cadastrarPeloProfissional(7L, requisicao(
				List.of(new HorarioFuncionamentoRequest(
						DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)))));

		assertEquals(profissional, criada.getProprietario());
		assertEquals(criada, profissional.getBarbearia());
		verify(horarioFuncionamentoRepository).saveAll(any());
	}

	@Test
	void naoDeveTransferirProprietarioComAgendamentoFuturo() {
		Usuario usuario = new Usuario("João", "joao@teste.com", "hash");
		Profissional profissional = new Profissional(usuario, new Barbearia("Antiga"));
		when(profissionalRepository.findByUsuarioIdAndAtivoTrue(7L)).thenReturn(java.util.Optional.of(profissional));
		when(repository.existsByNomeIgnoreCase("Nova Barbearia")).thenReturn(false);
		when(clock.instant()).thenReturn(Instant.parse("2030-01-01T12:00:00Z"));
		when(agendamentoRepository.existsByProfissionalIdAndStatusInAndInicioAfter(
				null, StatusAgendamento.QUE_OCUPAM_HORARIO, Instant.parse("2030-01-01T12:00:00Z")))
				.thenReturn(true);

		assertThrows(OperacaoBarbeariaConflitanteException.class,
				() -> service.cadastrarPeloProfissional(7L, requisicao(List.of(
						new HorarioFuncionamentoRequest(DayOfWeek.MONDAY,
								LocalTime.of(9, 0), LocalTime.of(18, 0))))));
		verify(repository, never()).save(any());
	}

	@Test
	void naoDeveAceitarFuncionamentosSobrepostos() {
		Usuario usuario = new Usuario("João", "joao@teste.com", "hash");
		Profissional profissional = new Profissional(usuario, new Barbearia("Antiga"));
		when(profissionalRepository.findByUsuarioIdAndAtivoTrue(7L)).thenReturn(java.util.Optional.of(profissional));
		when(repository.existsByNomeIgnoreCase("Nova Barbearia")).thenReturn(false);

		assertThrows(HorarioFuncionamentoSobrepostoException.class,
				() -> service.cadastrarPeloProfissional(7L, requisicao(List.of(
						new HorarioFuncionamentoRequest(DayOfWeek.MONDAY,
								LocalTime.of(9, 0), LocalTime.of(13, 0)),
						new HorarioFuncionamentoRequest(DayOfWeek.MONDAY,
								LocalTime.of(12, 0), LocalTime.of(18, 0))))));
	}

	@Test
	void deveTransferirPropriedadeSomenteParaMembroAtivoDaUnidade() {
		Barbearia barbearia = mock(Barbearia.class);
		Profissional novoProprietario = mock(Profissional.class);
		when(repository.findById(4L)).thenReturn(java.util.Optional.of(barbearia));
		when(barbearia.isAtivo()).thenReturn(true);
		when(profissionalRepository.findById(9L)).thenReturn(java.util.Optional.of(novoProprietario));
		when(novoProprietario.isAtivo()).thenReturn(true);
		when(novoProprietario.getBarbearia()).thenReturn(barbearia);
		when(barbearia.getId()).thenReturn(4L);

		assertEquals(barbearia, service.alterarProprietario(4L, 9L));

		verify(barbearia).transferirPropriedadePara(novoProprietario);
	}

	private CadastroBarbeariaRequest requisicao(List<HorarioFuncionamentoRequest> horarios) {
		return new CadastroBarbeariaRequest(
				" Nova Barbearia ",
				new EnderecoBarbeariaRequest(
						"22041001", "Rua Teste", "10", null,
						"Centro", "Rio de Janeiro", "RJ"),
				horarios
		);
	}
}
