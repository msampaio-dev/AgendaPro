package com.agendapro.agendamento.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.agendapro.agendamento.exception.TransicaoStatusAgendamentoInvalidaException;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.servico.entity.Servico;
import com.agendapro.usuario.entity.Usuario;

class AgendamentoStatusTest {

	@Test
	void deveConfirmarAgendamentoAgendado() {
		Agendamento agendamento = novoAgendamento();
		agendamento.confirmar();
		assertEquals(StatusAgendamento.CONFIRMADO, agendamento.getStatus());
	}

	@Test
	void deveCancelarAgendamentoAgendado() {
		Agendamento agendamento = novoAgendamento();
		agendamento.cancelar();
		assertEquals(StatusAgendamento.CANCELADO, agendamento.getStatus());
	}

	@Test
	void deveCancelarAgendamentoConfirmado() {
		Agendamento agendamento = novoAgendamento();
		agendamento.confirmar();
		agendamento.cancelar();
		assertEquals(StatusAgendamento.CANCELADO, agendamento.getStatus());
	}

	@Test
	void deveConcluirSomenteDepoisDeConfirmar() {
		Agendamento agendamento = novoAgendamento();
		assertThrows(TransicaoStatusAgendamentoInvalidaException.class, agendamento::concluir);

		agendamento.confirmar();
		agendamento.concluir();
		assertEquals(StatusAgendamento.CONCLUIDO, agendamento.getStatus());
	}

	@Test
	void naoDeveAlterarAgendamentoCancelado() {
		Agendamento agendamento = novoAgendamento();
		agendamento.cancelar();
		assertThrows(TransicaoStatusAgendamentoInvalidaException.class, agendamento::confirmar);
		assertThrows(TransicaoStatusAgendamentoInvalidaException.class, agendamento::concluir);
	}

	@Test
	void naoDeveAlterarAgendamentoConcluido() {
		Agendamento agendamento = novoAgendamento();
		agendamento.confirmar();
		agendamento.concluir();
		assertThrows(TransicaoStatusAgendamentoInvalidaException.class, agendamento::cancelar);
	}

	private Agendamento novoAgendamento() {
		Usuario cliente = new Usuario("Cliente", "cliente@agendapro.com");
		Profissional profissional = new Profissional(new Usuario("Profissional", "pro@agendapro.com"));
		Servico servico = new Servico("Corte", null, 30, new BigDecimal("50.00"));
		return new Agendamento(
				cliente,
				profissional,
				servico,
				Instant.parse("2030-01-07T12:00:00Z"),
				Instant.parse("2030-01-07T12:30:00Z"));
	}
}
