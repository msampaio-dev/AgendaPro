package com.agendapro.profissionalservico.entity;

import com.agendapro.profissional.entity.Profissional;
import com.agendapro.servico.entity.Servico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "profissionais_servicos",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_profissionais_servicos_profissional_servico",
				columnNames = {"profissional_id", "servico_id"}
		)
)
public class ProfissionalServico {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profissional_id", nullable = false)
	private Profissional profissional;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "servico_id", nullable = false)
	private Servico servico;

	@Column(nullable = false)
	private boolean ativo = true;

	protected ProfissionalServico() {
	}

	public ProfissionalServico(Profissional profissional, Servico servico) {
		this.profissional = profissional;
		this.servico = servico;
	}

	public Long getId() {
		return id;
	}

	public Profissional getProfissional() {
		return profissional;
	}

	public Servico getServico() {
		return servico;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public void desativar() {
		this.ativo = false;
	}

	public void ativar() {
		this.ativo = true;
	}
}
