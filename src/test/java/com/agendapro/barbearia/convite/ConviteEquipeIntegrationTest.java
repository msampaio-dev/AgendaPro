package com.agendapro.barbearia.convite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.agendapro.agendamento.repository.AgendamentoRepository;
import com.agendapro.barbearia.convite.entity.ConviteEquipe;
import com.agendapro.barbearia.convite.entity.StatusConviteEquipe;
import com.agendapro.barbearia.convite.exception.OperacaoConviteEquipeConflitanteException;
import com.agendapro.barbearia.convite.repository.ConviteEquipeRepository;
import com.agendapro.barbearia.convite.service.ConviteEquipeService;
import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.barbearia.repository.BarbeariaRepository;
import com.agendapro.profissional.repository.ProfissionalRepository;
import com.agendapro.shared.PostgresIntegrationTest;
import com.agendapro.usuario.entity.PerfilUsuario;
import com.agendapro.usuario.entity.Usuario;
import com.agendapro.profissionalservico.repository.ProfissionalServicoRepository;
import com.agendapro.servico.repository.ServicoRepository;
import com.agendapro.usuario.repository.UsuarioRepository;

@SpringBootTest
class ConviteEquipeIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	private ConviteEquipeService service;
	@Autowired
	private ConviteEquipeRepository conviteRepository;
	@Autowired
	private AgendamentoRepository agendamentoRepository;
	@Autowired
	private ProfissionalRepository profissionalRepository;
	@Autowired
	private BarbeariaRepository barbeariaRepository;

	@Autowired
	private ProfissionalServicoRepository profissionalServicoRepository;

	@Autowired
	private ServicoRepository servicoRepository;
	@Autowired
	private UsuarioRepository usuarioRepository;

	@BeforeEach
	void limparBanco() {
		// Na ordem inversa das chaves estrangeiras: o contexto do Spring e o banco
		// são compartilhados com as demais classes de teste de mesma configuração,
		// então pode haver agendamentos remanescentes apontando para profissionais.
		conviteRepository.deleteAll();
		agendamentoRepository.deleteAll();
		profissionalServicoRepository.deleteAll();
		servicoRepository.deleteAll();
		profissionalRepository.deleteAll();
		barbeariaRepository.deleteAll();
		usuarioRepository.deleteAll();
	}

	@Test
	void deveExecutarFluxoCompletoEImpedirSegundoAceite() {
		Usuario proprietario = usuarioRepository.save(
				new Usuario("Proprietário", "proprietario@teste.com", "hash"));
		proprietario.adicionarPerfil(PerfilUsuario.ADMIN);
		usuarioRepository.save(proprietario);
		Usuario convidado = usuarioRepository.save(
				new Usuario("Convidado", "convidado@teste.com", "hash"));
		Barbearia barbearia = barbeariaRepository.save(new Barbearia("Barbearia de teste"));

		ConviteEquipe convite = service.criar(
				barbearia.getId(), "  CONVIDADO@TESTE.COM ", proprietario.getId());
		Usuario usuarioAtualizado = service.aceitar(convite.getId(), convidado.getId());

		ConviteEquipe salvo = conviteRepository.findById(convite.getId()).orElseThrow();
		var profissional = profissionalRepository.findByUsuarioId(convidado.getId()).orElseThrow();
		assertEquals(StatusConviteEquipe.ACEITO, salvo.getStatus());
		assertEquals("convidado@teste.com", salvo.getEmail());
		assertEquals(barbearia.getId(), profissional.getBarbearia().getId());
		assertTrue(usuarioAtualizado.getPerfis().contains(PerfilUsuario.PROFISSIONAL));
		assertEquals(1, profissionalRepository.count());

		assertThrows(OperacaoConviteEquipeConflitanteException.class,
				() -> service.aceitar(convite.getId(), convidado.getId()));
		assertEquals(1, profissionalRepository.count());
	}

	@Test
	void bancoDeveImpedirDoisConvitesPendentesParaMesmoEmailEBarbearia() {
		Usuario proprietario = usuarioRepository.save(
				new Usuario("Proprietário", "dono@teste.com", "hash"));
		Barbearia barbearia = barbeariaRepository.save(new Barbearia("Barbearia única"));
		Instant agora = Instant.parse("2026-09-17T12:00:00Z");
		conviteRepository.saveAndFlush(new ConviteEquipe(
				barbearia, "pessoa@teste.com", proprietario, agora, agora.plusSeconds(3600)));

		assertThrows(DataIntegrityViolationException.class,
				() -> conviteRepository.saveAndFlush(new ConviteEquipe(
						barbearia, "PESSOA@TESTE.COM", proprietario, agora, agora.plusSeconds(3600))));
	}
}
