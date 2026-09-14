package com.agendapro.agendamento.entity;

import java.time.Instant;

import com.agendapro.agendamento.exception.TransicaoStatusAgendamentoInvalidaException;
import com.agendapro.profissional.entity.Profissional;
import com.agendapro.servico.entity.Servico;
import com.agendapro.usuario.entity.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "agendamentos")
public class Agendamento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cliente_id", nullable = false)
	private Usuario cliente;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profissional_id", nullable = false)
	private Profissional profissional;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "servico_id", nullable = false)
	private Servico servico;

	@Column(nullable = false)
	private Instant inicio;

	@Column(nullable = false)
	private Instant fim;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private StatusAgendamento status = StatusAgendamento.AGENDADO;

	protected Agendamento() {
	}

	public Agendamento(
			Usuario cliente,
			Profissional profissional,
			Servico servico,
			Instant inicio,
			Instant fim
	) {
		this.cliente = cliente;
		this.profissional = profissional;
		this.servico = servico;
		this.inicio = inicio;
		this.fim = fim;
	}

	public Long getId() {
		return id;
	}

	public Usuario getCliente() {
		return cliente;
	}

	public Profissional getProfissional() {
		return profissional;
	}

	public Servico getServico() {
		return servico;
	}

	public Instant getInicio() {
		return inicio;
	}

	public Instant getFim() {
		return fim;
	}

	public StatusAgendamento getStatus() {
		return status;
	}

	public void confirmar() {
		if (status != StatusAgendamento.AGENDADO) {
			throw new TransicaoStatusAgendamentoInvalidaException(
					status,
					StatusAgendamento.CONFIRMADO);
		}
		status = StatusAgendamento.CONFIRMADO;
	}

	public void cancelar() {
		if (status != StatusAgendamento.AGENDADO && status != StatusAgendamento.CONFIRMADO) {
			throw new TransicaoStatusAgendamentoInvalidaException(
					status,
					StatusAgendamento.CANCELADO);
		}
		status = StatusAgendamento.CANCELADO;
	}

	public void concluir() {
		if (status != StatusAgendamento.CONFIRMADO) {
			throw new TransicaoStatusAgendamentoInvalidaException(
					status,
					StatusAgendamento.CONCLUIDO);
		}
		status = StatusAgendamento.CONCLUIDO;
	}
}
