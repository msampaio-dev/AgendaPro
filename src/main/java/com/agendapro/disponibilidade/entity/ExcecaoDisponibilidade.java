package com.agendapro.disponibilidade.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import com.agendapro.profissional.entity.Profissional;

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
@Table(name = "excecoes_disponibilidade")
public class ExcecaoDisponibilidade {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profissional_id", nullable = false)
	private Profissional profissional;

	@Column(nullable = false)
	private LocalDate data;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private TipoExcecaoDisponibilidade tipo;

	@Column(name = "horario_inicio")
	private LocalTime horarioInicio;

	@Column(name = "horario_fim")
	private LocalTime horarioFim;

	@Column(nullable = false)
	private boolean ativo = true;

	protected ExcecaoDisponibilidade() {
	}

	public ExcecaoDisponibilidade(
			Profissional profissional,
			LocalDate data,
			TipoExcecaoDisponibilidade tipo,
			LocalTime horarioInicio,
			LocalTime horarioFim
	) {
		this.profissional = profissional;
		this.data = data;
		this.tipo = tipo;
		this.horarioInicio = horarioInicio;
		this.horarioFim = horarioFim;
	}

	public Long getId() {
		return id;
	}

	public Profissional getProfissional() {
		return profissional;
	}

	public LocalDate getData() {
		return data;
	}

	public TipoExcecaoDisponibilidade getTipo() {
		return tipo;
	}

	public LocalTime getHorarioInicio() {
		return horarioInicio;
	}

	public LocalTime getHorarioFim() {
		return horarioFim;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public boolean isDiaInteiro() {
		return horarioInicio == null && horarioFim == null;
	}

	public void desativar() {
		this.ativo = false;
	}
}
