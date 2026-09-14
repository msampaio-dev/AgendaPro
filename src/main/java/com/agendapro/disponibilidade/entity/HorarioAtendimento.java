package com.agendapro.disponibilidade.entity;

import java.time.DayOfWeek;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "horarios_atendimento",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_horarios_atendimento_intervalo",
				columnNames = {
						"profissional_id",
						"dia_semana",
						"horario_inicio",
						"horario_fim"
				}
		)
)
public class HorarioAtendimento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profissional_id", nullable = false)
	private Profissional profissional;

	@Enumerated(EnumType.STRING)
	@Column(name = "dia_semana", nullable = false, length = 9)
	private DayOfWeek diaSemana;

	@Column(name = "horario_inicio", nullable = false)
	private LocalTime horarioInicio;

	@Column(name = "horario_fim", nullable = false)
	private LocalTime horarioFim;

	@Column(nullable = false)
	private boolean ativo = true;

	protected HorarioAtendimento() {
	}

	public HorarioAtendimento(
			Profissional profissional,
			DayOfWeek diaSemana,
			LocalTime horarioInicio,
			LocalTime horarioFim
	) {
		this.profissional = profissional;
		this.diaSemana = diaSemana;
		this.horarioInicio = horarioInicio;
		this.horarioFim = horarioFim;
	}

	public Long getId() {
		return id;
	}

	public Profissional getProfissional() {
		return profissional;
	}

	public DayOfWeek getDiaSemana() {
		return diaSemana;
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

	public void desativar() {
		this.ativo = false;
	}
}
